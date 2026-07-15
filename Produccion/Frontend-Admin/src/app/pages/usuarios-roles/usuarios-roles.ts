import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PersonalService } from '../../core/services/personal.service';
import { ToastService } from '../../core/services/toast.service';
import { PersonalRequest, PersonalResumen, RolStaff, etiquetaRol, iniciales } from '../../core/models/admin.models';

const ROLES_DISPONIBLES: RolStaff[] = [
  'RECEPCIONISTA',
  'ANALISTA_PRIMER_CONTACTO',
  'SUBDEFENSOR',
  'DEFENSOR',
  'ADMIN_SISTEMAS',
];

@Component({
  selector: 'app-usuarios-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios-roles.html',
  styleUrl: './usuarios-roles.scss',
})
export class UsuariosRoles implements OnInit {
  readonly roles = ROLES_DISPONIBLES;
  readonly etiquetaRol = etiquetaRol;
  readonly iniciales = iniciales;

  lista: PersonalResumen[] = [];
  cargando = true;
  busqueda = '';

  mostrarModalCrear = false;
  mostrarModalEditar = false;
  mostrarModalPasswordGenerada = false;

  formulario: PersonalRequest = this.formularioVacio();
  editandoId: number | null = null;
  passwordGenerada = '';
  correoGenerado = '';

  constructor(
    private personalService: PersonalService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarLista();
  }

  get listaFiltrada(): PersonalResumen[] {
    const termino = this.busqueda.trim().toLowerCase();
    if (!termino) return this.lista;
    return this.lista.filter((p) =>
      p.nombreCompleto.toLowerCase().includes(termino) ||
      p.correoInstitucional.toLowerCase().includes(termino) ||
      p.numeroEmpleado.toLowerCase().includes(termino) ||
      this.etiquetaRol(p.rol).toLowerCase().includes(termino),
    );
  }

  private cargarLista(): void {
    this.cargando = true;
    this.personalService.listar().subscribe({
      next: (lista) => {
        this.lista = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar la lista de personal.');
        this.cdr.detectChanges();
      },
    });
  }

  abrirCrear(): void {
    this.formulario = this.formularioVacio();
    this.mostrarModalCrear = true;
  }

  cerrarCrear(): void {
    this.mostrarModalCrear = false;
  }

  guardarNuevo(): void {
    if (!this.formulario.nombreCompleto || !this.formulario.numeroEmpleado
        || !this.formulario.correoInstitucional || !this.formulario.rol) {
      this.toast.advertencia('Completa nombre, número de empleado, correo y rol.');
      return;
    }

    this.personalService.crear(this.formulario).subscribe({
      next: (creado) => {
        this.mostrarModalCrear = false;
        this.correoGenerado = creado.correoInstitucional;
        this.passwordGenerada = creado.passwordTemporal;
        this.mostrarModalPasswordGenerada = true;
        this.toast.exito('Usuario creado correctamente.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo crear el usuario.');
        this.cdr.detectChanges();
      },
    });
  }

  abrirEditar(personal: PersonalResumen): void {
    this.editandoId = personal.id;
    this.formulario = {
      nombreCompleto: personal.nombreCompleto,
      correoInstitucional: personal.correoInstitucional,
      rol: personal.rol,
      restablecerPassword: false,
      desactivarTemporalmente: false,
    };
    this.mostrarModalEditar = true;
  }

  cerrarEditar(): void {
    this.mostrarModalEditar = false;
    this.editandoId = null;
  }

  guardarEdicion(): void {
    if (this.editandoId == null) return;
    this.personalService.editar(this.editandoId, this.formulario).subscribe({
      next: () => {
        this.mostrarModalEditar = false;
        this.editandoId = null;
        this.toast.exito('Información actualizada.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo actualizar la información.');
        this.cdr.detectChanges();
      },
    });
  }

  resetearPassword(personal: PersonalResumen): void {
    if (!confirm(`¿Generar una nueva contraseña temporal para ${personal.nombreCompleto}?`)) return;
    this.personalService.resetearPassword(personal.id).subscribe({
      next: (resp) => {
        this.correoGenerado = personal.correoInstitucional;
        this.passwordGenerada = resp.passwordTemporalNueva;
        this.mostrarModalPasswordGenerada = true;
        this.toast.exito('Contraseña restablecida.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: () => {
        this.toast.error('No se pudo restablecer la contraseña.');
        this.cdr.detectChanges();
      },
    });
  }

  darDeBaja(personal: PersonalResumen): void {
    if (!confirm(`¿Está seguro de dar de baja a ${personal.nombreCompleto}? Perderá acceso inmediato al sistema.`)) {
      return;
    }
    this.personalService.darDeBaja(personal.id).subscribe({
      next: () => {
        this.toast.exito('Usuario dado de baja.');
        this.cargarLista();
      },
      error: () => {
        this.toast.error('No se pudo dar de baja al usuario.');
        this.cdr.detectChanges();
      },
    });
  }

  reactivar(personal: PersonalResumen): void {
    this.personalService.reactivar(personal.id).subscribe({
      next: () => {
        this.toast.exito('Usuario reactivado.');
        this.cargarLista();
      },
      error: () => {
        this.toast.error('No se pudo reactivar al usuario.');
        this.cdr.detectChanges();
      },
    });
  }

  cerrarModalPassword(): void {
    this.mostrarModalPasswordGenerada = false;
    this.passwordGenerada = '';
  }

  private formularioVacio(): PersonalRequest {
    return {
      nombreCompleto: '',
      numeroEmpleado: '',
      correoInstitucional: '',
      rol: 'RECEPCIONISTA',
      passwordTemporal: '',
    };
  }
}
