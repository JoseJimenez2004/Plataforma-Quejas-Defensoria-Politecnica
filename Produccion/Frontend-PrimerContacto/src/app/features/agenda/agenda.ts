import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';

import { AgendaService } from '../../core/services/agenda.service';
import { CitaDetalleDialog } from '../../shared/cita-detalle-dialog/cita-detalle-dialog';
import { NuevaCita } from '../../shared/nueva-cita/nueva-cita';
import { mensajeDeError } from '../../core/utils/archivos';
import {
  CATEGORIAS_CITA,
  CitaPrimerContacto,
  citaOcupaHorario,
  etiquetaCategoria
} from '../../core/models/cita-primer-contacto';

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre',
  'octubre', 'noviembre', 'diciembre'];
const TAMANIO = 10;

interface FiltroCitas {
  estado: string;
  categoria: string;
  modalidad: string;
  desde: string;
  hasta: string;
}

type CampoOrden = 'fecha' | 'hora' | 'folio' | 'quejoso' | 'categoria' | 'tipo' | 'estatus';

/**
 * Agenda de Citas (diseño 2026-10-18): indicadores, calendario institucional del mes con las
 * citas del día elegido, "Mis próximas citas", filtros, lista ordenable con exportar a CSV y la
 * ventana "Programar Nueva Cita". Confirmar, reagendar y cancelar siguen en el detalle de la cita.
 */
@Component({
  selector: 'app-agenda',
  imports: [FormsModule, MatIconModule, MatSnackBarModule, MatDialogModule, NuevaCita],
  templateUrl: './agenda.html',
  styleUrl: './agenda.css'
})
export class Agenda implements OnInit {
  readonly diasSemana = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  readonly categorias = CATEGORIAS_CITA;
  readonly etiquetaCategoria = etiquetaCategoria;

  vista: 'calendario' | 'lista' = 'calendario';

  // calendario
  anio = new Date().getFullYear();
  mes = new Date().getMonth();
  diaSel = iso(new Date());
  citasMes: CitaPrimerContacto[] = [];
  proximas: CitaPrimerContacto[] = [];

  // indicadores (de hace 30 días a 90 días adelante)
  citasVentana: CitaPrimerContacto[] = [];

  // lista
  filtro: FiltroCitas = { estado: '', categoria: '', modalidad: '', desde: '', hasta: '' };
  citasLista: CitaPrimerContacto[] = [];
  cargandoLista = false;
  orden: { campo: CampoOrden; asc: boolean } = { campo: 'fecha', asc: true };
  pagina = 0;
  menuAbierto: number | null = null;

  // ventana de nueva cita
  mostrarNueva = false;
  folioNueva = '';
  citaReagendar?: CitaPrimerContacto;

  constructor(
    private agenda: AgendaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    // Desde un expediente: abrir directo la ventana con ese folio.
    const folio = history.state?.folio;
    if (folio) {
      this.folioNueva = folio;
      this.mostrarNueva = true;
    }
  }

  ngOnInit(): void {
    this.recargarTodo();
  }

  @HostListener('document:click')
  cerrarMenu(): void {
    this.menuAbierto = null;
  }

  private recargarTodo(): void {
    this.cargarMes();
    this.cargarVentana();
    this.agenda.obtenerMisProximasCitas().subscribe({
      next: (c) => { this.proximas = c; this.cdr.detectChanges(); }
    });
    if (this.vista === 'lista') {
      this.cargarLista();
    }
  }

  // ------------------------------------------------------------------ indicadores

  private cargarVentana(): void {
    const hoy = new Date();
    this.agenda.obtenerRango(iso(sumarDias(hoy, -30)), iso(sumarDias(hoy, 90))).subscribe({
      next: (c) => { this.citasVentana = c; this.cdr.detectChanges(); }
    });
  }

  get hoyIso(): string {
    return iso(new Date());
  }

  get kpiHoy(): number {
    return this.citasVentana.filter(c => isoDeVista(c.fecha) === this.hoyIso && citaOcupaHorario(c)).length;
  }

  get kpiProximas(): number {
    const ahora = Date.now();
    return this.citasVentana.filter(c => citaOcupaHorario(c) && fechaHora(c) > ahora).length;
  }

  get kpiConfirmadas(): number {
    const ahora = Date.now();
    return this.citasVentana.filter(c => c.estatusCodigo === 'CONFIRMADA' && fechaHora(c) > ahora).length;
  }

  get kpiCanceladas(): number {
    return this.citasVentana.filter(c => c.estatusCodigo === 'CANCELADA' || c.estatusCodigo === 'CANCELADA_QUEJOSO').length;
  }

  // ------------------------------------------------------------------ calendario

  get tituloMes(): string {
    const m = MESES[this.mes];
    return `${m.charAt(0).toUpperCase() + m.slice(1)} ${this.anio}`;
  }

  get celdas(): (number | null)[] {
    const primero = new Date(this.anio, this.mes, 1).getDay();
    const dias = new Date(this.anio, this.mes + 1, 0).getDate();
    const r: (number | null)[] = [...Array(primero).fill(null), ...Array.from({ length: dias }, (_, i) => i + 1)];
    while (r.length % 7) r.push(null);
    return r;
  }

  isoDe(dia: number): string {
    return iso(new Date(this.anio, this.mes, dia));
  }

  citasDe(fechaIso: string): CitaPrimerContacto[] {
    return this.citasMes.filter(c => isoDeVista(c.fecha) === fechaIso);
  }

  /** Para el punto del calendario: solo citas que siguen en pie. */
  tieneCitas(dia: number): boolean {
    return this.citasDe(this.isoDe(dia)).some(c => citaOcupaHorario(c));
  }

  get citasDelDia(): CitaPrimerContacto[] {
    return this.citasDe(this.diaSel);
  }

  get tituloDia(): string {
    const [a, m, d] = this.diaSel.split('-').map(Number);
    return `${d} de ${MESES[m - 1]} de ${a}`;
  }

  mover(delta: number): void {
    const d = new Date(this.anio, this.mes + delta, 1);
    this.anio = d.getFullYear();
    this.mes = d.getMonth();
    this.diaSel = iso(d);
    this.cargarMes();
  }

  irAHoy(): void {
    const hoy = new Date();
    this.anio = hoy.getFullYear();
    this.mes = hoy.getMonth();
    this.diaSel = iso(hoy);
    this.cargarMes();
  }

  private cargarMes(): void {
    const desde = iso(new Date(this.anio, this.mes, 1));
    const hasta = iso(new Date(this.anio, this.mes + 1, 0));
    this.agenda.obtenerRango(desde, hasta).subscribe({
      next: (c) => { this.citasMes = c; this.cdr.detectChanges(); },
      error: () => this.snackBar.open('No fue posible cargar la agenda del mes.', 'Cerrar', { duration: 3000 })
    });
  }

  // ------------------------------------------------------------------ lista

  cambiarVista(v: 'calendario' | 'lista'): void {
    this.vista = v;
    if (v === 'lista') {
      this.cargarLista();
    }
  }

  /** "Aplicar filtros" (desde el calendario o la lista) abre la lista ya filtrada. */
  aplicarFiltros(): void {
    if (this.filtro.desde && this.filtro.hasta && this.filtro.hasta < this.filtro.desde) {
      this.snackBar.open('La fecha de fin no puede ser anterior a la de inicio.', 'Cerrar', { duration: 3000 });
      return;
    }
    this.pagina = 0;
    this.cambiarVista('lista');
  }

  limpiarFiltros(): void {
    this.filtro = { estado: '', categoria: '', modalidad: '', desde: '', hasta: '' };
    this.aplicarFiltros();
  }

  private cargarLista(): void {
    const hoy = new Date();
    const desde = this.filtro.desde || iso(sumarDias(hoy, -30));
    const hasta = this.filtro.hasta || iso(sumarDias(new Date(desde + 'T00:00:00'), 120));
    this.cargandoLista = true;
    this.agenda.obtenerRango(desde, hasta).subscribe({
      next: (c) => { this.citasLista = c; this.cargandoLista = false; this.cdr.detectChanges(); },
      error: (e) => {
        this.cargandoLista = false;
        this.snackBar.open(mensajeDeError(e, 'No fue posible cargar las citas.'), 'Cerrar', { duration: 3500 });
        this.cdr.detectChanges();
      }
    });
  }

  get filtradas(): CitaPrimerContacto[] {
    const f = this.filtro;
    const lista = this.citasLista.filter(c =>
      (!f.estado || (f.estado === 'CANCELADA' ? c.estatusCodigo?.startsWith('CANCELADA') : c.estatusCodigo === f.estado))
      && (!f.categoria || (c.categoria ?? 'COMPARECENCIA') === f.categoria)
      && (!f.modalidad || c.tipo.toUpperCase() === f.modalidad));
    const valor = (c: CitaPrimerContacto): string => {
      switch (this.orden.campo) {
        case 'fecha': return isoDeVista(c.fecha) + c.hora;
        case 'hora': return c.hora;
        case 'folio': return c.folio;
        case 'quejoso': return c.quejoso ?? '';
        case 'categoria': return etiquetaCategoria(c.categoria);
        case 'tipo': return c.tipo;
        default: return c.estatus;
      }
    };
    return [...lista].sort((a, b) => valor(a).localeCompare(valor(b)) * (this.orden.asc ? 1 : -1));
  }

  get visibles(): CitaPrimerContacto[] {
    return this.filtradas.slice(this.pagina * TAMANIO, (this.pagina + 1) * TAMANIO);
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.filtradas.length / TAMANIO));
  }

  get paginas(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, i) => i);
  }

  get rango(): string {
    const total = this.filtradas.length;
    if (!total) return 'Sin citas';
    const desde = this.pagina * TAMANIO + 1;
    return `Mostrando ${desde} a ${desde + this.visibles.length - 1} de ${total} citas`;
  }

  ordenarPor(campo: CampoOrden): void {
    this.orden = { campo, asc: this.orden.campo === campo ? !this.orden.asc : true };
  }

  flecha(campo: CampoOrden): string {
    return this.orden.campo !== campo ? 'unfold_more' : this.orden.asc ? 'expand_less' : 'expand_more';
  }

  exportar(): void {
    const filas = this.filtradas;
    const celda = (v: string | number | undefined | null) => `"${String(v ?? '').replace(/"/g, '""').replace(/\r?\n/g, ' ')}"`;
    const lineas = [['Fecha', 'Hora', 'Expediente', 'Persona citada', 'Tipo de cita', 'Modalidad', 'Estado', 'Motivo', 'Agendó']]
      .concat(filas.map(c => [c.fecha, c.hora, c.folio, c.quejoso, etiquetaCategoria(c.categoria), c.tipo, c.estatus, c.motivo, c.analistaNombre ?? '']))
      .map(f => f.map(celda).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob(['﻿' + lineas], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `citas-${this.hoyIso}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  }

  alternarMenu(id: number | undefined, e: Event): void {
    e.stopPropagation();
    this.menuAbierto = this.menuAbierto === id ? null : id ?? null;
  }

  // ------------------------------------------------------------------ acciones

  nuevaCita(): void {
    this.citaReagendar = undefined;
    this.folioNueva = '';
    this.mostrarNueva = true;
  }

  get fechaParaNueva(): string {
    return this.diaSel >= this.hoyIso ? this.diaSel : '';
  }

  citaGuardada(): void {
    this.mostrarNueva = false;
    this.citaReagendar = undefined;
    this.recargarTodo();
  }

  abrirDetalle(cita: CitaPrimerContacto): void {
    this.menuAbierto = null;
    this.dialog.open(CitaDetalleDialog, { width: '720px', data: cita }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.ejecutar(resultado.accion, resultado.cita, resultado.motivo);
      }
    });
  }

  /** Mismas acciones que el detalle, también desde el menú ⋮ de la lista. */
  ejecutar(accion: string, cita: CitaPrimerContacto, motivo?: string): void {
    this.menuAbierto = null;
    if (!cita.id) return;
    if (accion === 'reagendar') {
      this.citaReagendar = cita;
      this.mostrarNueva = true;
      return;
    }
    let operacion: Observable<unknown>;
    let mensaje: string;
    if (accion === 'confirmar') {
      operacion = this.agenda.confirmarCita(cita.id);
      mensaje = 'Cita confirmada. Se avisó al quejoso.';
    } else if (accion === 'cancelacion-quejoso' && motivo) {
      operacion = this.agenda.registrarCancelacionQuejoso(cita.id, motivo);
      mensaje = 'Cancelación del quejoso registrada. Reagenda o cancela la cita.';
    } else if (accion === 'cancelar') {
      if (!confirm('¿Cancelar la cita? Se le avisará al quejoso.')) return;
      operacion = this.agenda.cancelarCita(cita.id);
      mensaje = 'Cita cancelada. Se avisó al quejoso.';
    } else {
      return;
    }
    operacion.subscribe({
      next: () => {
        this.snackBar.open(mensaje, 'Cerrar', { duration: 3000 });
        this.recargarTodo();
      },
      error: (e) => this.snackBar.open(mensajeDeError(e, 'No fue posible completar la acción.'), 'Cerrar', { duration: 3500 })
    });
  }

  puedeConfirmar(c: CitaPrimerContacto): boolean {
    return c.estatusCodigo === 'PROGRAMADA' || c.estatusCodigo === 'SIN_RESPUESTA';
  }

  activa(c: CitaPrimerContacto): boolean {
    return citaOcupaHorario(c) || c.estatusCodigo === 'CANCELADA_QUEJOSO';
  }

  diaCorto(c: CitaPrimerContacto): string {
    return c.fecha.slice(0, 2);
  }

  mesCorto(c: CitaPrimerContacto): string {
    return MESES[Number(c.fecha.slice(3, 5)) - 1]?.slice(0, 3).toUpperCase() ?? '';
  }
}

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function sumarDias(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

/** 'dd/MM/yyyy' (formato de la vista) -> 'yyyy-MM-dd'. */
function isoDeVista(fecha: string): string {
  const [d, m, a] = fecha.split('/');
  return `${a}-${m}-${d}`;
}

function fechaHora(c: CitaPrimerContacto): number {
  return new Date(`${isoDeVista(c.fecha)}T${c.hora || '00:00'}:00`).getTime();
}
