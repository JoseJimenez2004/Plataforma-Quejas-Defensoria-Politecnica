import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, of, switchMap } from 'rxjs';

import { PersonalService } from '../../core/services/personal.service';
import { SeguimientoService } from '../../core/services/seguimiento.service';
import { CatalogoAdminService } from '../../core/services/catalogo-admin.service';
import { ToastService } from '../../core/services/toast.service';
import {
  Dependencia,
  DetallePersonal,
  MiembroPersonal,
  PersonalRequest,
  RolStaff,
  etiquetaRolPersona,
  iniciales,
} from '../../core/models/admin.models';
import { Paginador } from '../../shared/panel/paginador';
import { fechaCorta, fechaHora } from '../../shared/panel/csv';

const ROLES_DISPONIBLES: RolStaff[] = ['RECEPCIONISTA', 'ANALISTA_PRIMER_CONTACTO', 'SUBDEFENSOR', 'DEFENSOR', 'ADMIN_SISTEMAS'];

/** Área por defecto cuando la persona no tiene una dependencia capturada. */
const AREA_POR_ROL: Record<string, string> = {
  RECEPCIONISTA: 'Recepción',
  ANALISTA_PRIMER_CONTACTO: 'Primer Contacto',
  SUBDEFENSOR: 'Subdefensoría',
  DEFENSOR: 'Defensoría',
  ADMIN_SISTEMAS: 'Sistemas',
};

const TAMANIO = 10;

interface Formulario extends PersonalRequest {
  unidadClave?: string | null;
  telefono?: string | null;
}

/**
 * Personal de la Defensoría (diseño 2026-10-18): indicadores, filtros, tabla y panel lateral
 * con datos, actividad y quejas de cada persona. Alta, edición, contraseña y baja usan
 * /api/admin/personal; el área y el teléfono, /api/admin/seguimiento/personal/{id}/contacto.
 */
@Component({
  selector: 'app-usuarios-roles',
  standalone: true,
  imports: [FormsModule, RouterLink, Paginador],
  templateUrl: './usuarios-roles.html',
  styleUrl: './usuarios-roles.scss',
})
export class UsuariosRoles implements OnInit {
  readonly roles = ROLES_DISPONIBLES;
  readonly iniciales = iniciales;
  readonly fechaHora = fechaHora;
  readonly fechaCorta = fechaCorta;

  lista: MiembroPersonal[] = [];
  dependencias: Dependencia[] = [];
  cargando = true;

  texto = '';
  rol = '';
  estatus = '';
  unidad = '';
  pagina = 0;
  menuAbierto: number | null = null;

  // panel lateral
  seleccionado: MiembroPersonal | null = null;
  detalle: DetallePersonal | null = null;
  pestana: 'info' | 'actividad' | 'quejas' = 'info';

  // ventanas
  mostrarModalCrear = false;
  mostrarModalEditar = false;
  mostrarModalPasswordGenerada = false;
  guardando = false;
  formulario: Formulario = this.formularioVacio();
  editandoId: number | null = null;
  passwordGenerada = '';
  correoGenerado = '';

  constructor(
    private personalService: PersonalService,
    private seguimiento: SeguimientoService,
    private catalogo: CatalogoAdminService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarLista();
    this.catalogo.listarTodas().subscribe({
      next: (d) => {
        this.dependencias = (Array.isArray(d) ? d : []).filter((x) => x.activo !== false)
          .sort((a, b) => (a.abreviatura || a.nombre).localeCompare(b.abreviatura || b.nombre));
        this.cdr.detectChanges();
      },
      error: () => { /* sin catálogo se puede seguir sin área */ },
    });
  }

  @HostListener('document:click')
  cerrarMenu(): void {
    this.menuAbierto = null;
  }

  @HostListener('document:keydown.escape')
  alEscape(): void {
    if (!this.mostrarModalCrear && !this.mostrarModalEditar && !this.mostrarModalPasswordGenerada) {
      this.cerrarDetalle();
    }
  }

  // ------------------------------------------------------------------ datos

  private cargarLista(recargarDetalle = false): void {
    this.seguimiento.personal().subscribe({
      next: (l) => {
        this.lista = l;
        this.cargando = false;
        if (this.seleccionado) {
          this.seleccionado = l.find((m) => m.id === this.seleccionado?.id) ?? null;
          if (recargarDetalle && this.seleccionado) {
            this.verDetalle(this.seleccionado, this.pestana);
          }
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar la lista de personal.');
        this.cdr.detectChanges();
      },
    });
  }

  // ------------------------------------------------------------------ indicadores

  get activos(): number {
    return this.lista.filter((m) => m.activo).length;
  }

  get inactivos(): number {
    return this.lista.length - this.activos;
  }

  pct(n: number): number {
    return this.lista.length ? Math.round((n / this.lista.length) * 100) : 0;
  }

  contar(rol: string): number {
    return this.lista.filter((m) => m.rol === rol).length;
  }

  get rolesEnUso(): string[] {
    return ROLES_DISPONIBLES.filter((r) => this.contar(r) > 0);
  }

  get desgloseRoles(): string {
    const plural = (n: number, s: string, p: string) => `${n} ${n === 1 ? s : p}`;
    const partes = [
      plural(this.contar('RECEPCIONISTA'), 'recepcionista', 'recepcionistas'),
      plural(this.contar('SUBDEFENSOR'), 'abogado', 'abogados'),
      plural(this.contar('ANALISTA_PRIMER_CONTACTO'), 'analista', 'analistas'),
      plural(this.contar('ADMIN_SISTEMAS'), 'administrativo', 'administrativos'),
      plural(this.contar('DEFENSOR'), 'defensor', 'defensores'),
    ];
    return partes.filter((p) => !p.startsWith('0 ')).join(' · ');
  }

  // ------------------------------------------------------------------ filtros

  get filtradas(): MiembroPersonal[] {
    const t = this.texto.trim().toLowerCase();
    return this.lista.filter((m) =>
      (!t || m.nombre.toLowerCase().includes(t) || (m.correo ?? '').toLowerCase().includes(t)
        || (m.numeroEmpleado ?? '').toLowerCase().includes(t))
      && (!this.rol || m.rol === this.rol)
      && (!this.estatus || (this.estatus === 'ACTIVO') === m.activo)
      && (!this.unidad || this.area(m) === this.unidad));
  }

  get visibles(): MiembroPersonal[] {
    return this.filtradas.slice(this.pagina * TAMANIO, (this.pagina + 1) * TAMANIO);
  }

  get tamanio(): number {
    return TAMANIO;
  }

  get areas(): string[] {
    return [...new Set(this.lista.map((m) => this.area(m)))].sort((a, b) => a.localeCompare(b));
  }

  area(m: MiembroPersonal): string {
    return m.unidadNombre || AREA_POR_ROL[m.rol] || '—';
  }

  rolDe(m: MiembroPersonal): string {
    return etiquetaRolPersona(m.rol, m.nombre);
  }

  aplicar(): void {
    this.pagina = 0;
  }

  limpiar(): void {
    this.texto = '';
    this.rol = '';
    this.estatus = '';
    this.unidad = '';
    this.pagina = 0;
  }

  // ------------------------------------------------------------------ panel lateral

  verDetalle(m: MiembroPersonal, pestana: 'info' | 'actividad' | 'quejas' = 'info'): void {
    this.seleccionado = m;
    this.pestana = pestana;
    if (this.detalle?.persona.id !== m.id) {
      this.detalle = null;
    }
    this.seguimiento.detallePersonal(m.id).subscribe({
      next: (d) => { this.detalle = d; this.cdr.detectChanges(); },
      error: () => { this.toast.error('No se pudo cargar el detalle.'); this.cdr.detectChanges(); },
    });
  }

  cerrarDetalle(): void {
    this.seleccionado = null;
    this.detalle = null;
  }

  esAbogado(m: MiembroPersonal): boolean {
    return m.rol === 'SUBDEFENSOR' || m.rol === 'DEFENSOR';
  }

  verQuejasDe(m: MiembroPersonal): void {
    const queryParams = this.esAbogado(m) ? { abogado: m.nombre } : { recepcionista: m.correo };
    this.router.navigate(['/quejas'], { queryParams });
  }

  alternarMenu(id: number, e: Event): void {
    e.stopPropagation();
    this.menuAbierto = this.menuAbierto === id ? null : id;
  }

  // ------------------------------------------------------------------ alta y edición

  abrirCrear(): void {
    this.formulario = this.formularioVacio();
    this.mostrarModalCrear = true;
  }

  guardarNuevo(): void {
    const f = this.formulario;
    if (!f.nombreCompleto || !f.numeroEmpleado || !f.correoInstitucional || !f.rol) {
      this.toast.advertencia('Completa nombre, número de empleado, correo y rol.');
      return;
    }
    this.guardando = true;
    this.personalService.crear(f).pipe(
      switchMap((creado) => this.guardarContactoSiHay(creado.id, f).pipe(switchMap(() => of(creado)))),
    ).subscribe({
      next: (creado) => {
        this.guardando = false;
        this.mostrarModalCrear = false;
        this.correoGenerado = creado.correoInstitucional;
        this.passwordGenerada = creado.passwordTemporal;
        this.mostrarModalPasswordGenerada = true;
        this.toast.exito('Usuario creado correctamente.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo crear el usuario.');
        this.cdr.detectChanges();
      },
    });
  }

  abrirEditar(m: MiembroPersonal): void {
    this.menuAbierto = null;
    this.editandoId = m.id;
    this.formulario = {
      nombreCompleto: m.nombre,
      correoInstitucional: m.correo,
      rol: m.rol,
      unidadClave: m.unidadClave ?? '',
      telefono: m.telefono ?? '',
      restablecerPassword: false,
      desactivarTemporalmente: false,
    };
    this.mostrarModalEditar = true;
  }

  guardarEdicion(): void {
    if (this.editandoId == null) return;
    const id = this.editandoId;
    const f = this.formulario;
    this.guardando = true;
    this.personalService.editar(id, f).pipe(
      switchMap(() => this.seguimiento.guardarContacto(id, { unidadClave: f.unidadClave || null, telefono: f.telefono || null })),
    ).subscribe({
      next: () => {
        this.guardando = false;
        this.mostrarModalEditar = false;
        this.editandoId = null;
        this.toast.exito('Información actualizada.');
        this.cargarLista(true);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo actualizar la información.');
        this.cdr.detectChanges();
      },
    });
  }

  private guardarContactoSiHay(id: number, f: Formulario): Observable<unknown> {
    if (!f.unidadClave && !f.telefono) {
      return of(null);
    }
    return this.seguimiento.guardarContacto(id, { unidadClave: f.unidadClave || null, telefono: f.telefono || null });
  }

  resetearPassword(m: MiembroPersonal): void {
    this.menuAbierto = null;
    if (!confirm(`¿Generar una nueva contraseña temporal para ${m.nombre}?`)) return;
    this.personalService.resetearPassword(m.id).subscribe({
      next: (resp) => {
        this.correoGenerado = m.correo;
        this.passwordGenerada = resp.passwordTemporalNueva;
        this.mostrarModalPasswordGenerada = true;
        this.toast.exito('Contraseña restablecida.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: () => { this.toast.error('No se pudo restablecer la contraseña.'); this.cdr.detectChanges(); },
    });
  }

  darDeBaja(m: MiembroPersonal): void {
    this.menuAbierto = null;
    if (!confirm(`¿Desactivar a ${m.nombre}? Perderá el acceso al sistema de inmediato.`)) return;
    this.personalService.darDeBaja(m.id).subscribe({
      next: () => { this.toast.exito('Usuario desactivado.'); this.cargarLista(true); },
      error: (err) => { this.toast.error(err?.error?.mensaje ?? 'No se pudo desactivar al usuario.'); this.cdr.detectChanges(); },
    });
  }

  reactivar(m: MiembroPersonal): void {
    this.menuAbierto = null;
    this.personalService.reactivar(m.id).subscribe({
      next: () => { this.toast.exito('Usuario reactivado.'); this.cargarLista(true); },
      error: () => { this.toast.error('No se pudo reactivar al usuario.'); this.cdr.detectChanges(); },
    });
  }

  cerrarModalPassword(): void {
    this.mostrarModalPasswordGenerada = false;
    this.passwordGenerada = '';
  }

  etiquetaRolForm(r: string): string {
    return etiquetaRolPersona(r);
  }

  private formularioVacio(): Formulario {
    return { nombreCompleto: '', numeroEmpleado: '', correoInstitucional: '', rol: 'RECEPCIONISTA', passwordTemporal: '', unidadClave: '', telefono: '' };
  }
}
