import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { QuejaService } from '../../../core/services/queja.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import {
  EvidenciaResumen,
  PasoTramite,
  Queja,
  claseEstatus,
  esEditable,
  puedeCorregirse,
  etiquetaEstatus,
  pasosDelTramite,
} from '../../../core/models/queja.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { ToastService } from '../../../core/services/toast.service';
import { Datepicker } from '../../../shared/datepicker/datepicker';
import { AutocompletarDependencia } from '../../../shared/autocompletar-dependencia/autocompletar-dependencia';
import { ICONOS } from '../../../shared/iconos/iconos';
import * as Reglas from '../../../core/validaciones/reglas-queja';

/** Una evidencia junto con la miniatura que se pudo cargar de ella. */
interface EvidenciaConVista {
  evidencia: EvidenciaResumen;
  /** Object URL de la imagen; null si no es imagen o si aún no se ha descargado. */
  url: string | null;
  cargando: boolean;
}

@Component({
  selector: 'app-queja-detalle',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    Datepicker,
    AutocompletarDependencia,
    LucideAngularModule,
  ],
  templateUrl: './queja-detalle.html',
  styleUrl: './queja-detalle.scss',
})
export class QuejaDetalle implements OnInit, OnDestroy {
  readonly ICONOS = ICONOS;

  folio = '';
  cargando = true;
  error = '';
  queja: Queja | null = null;
  evidencias: EvidenciaConVista[] = [];
  dependencias: Dependencia[] = [];

  editando = false;
  guardando = false;
  readonly fechaMaxima = Reglas.hoyIso();

  // Campos del formulario de edición (copia de trabajo, no se toca "queja" hasta guardar).
  formDescripcion = '';
  formUnidadAcademica = '';
  formFechaHechos = '';
  formNombreDenunciado = '';
  formApellido1Denunciado = '';
  formApellido2Denunciado = '';

  // Gestión de evidencias
  subiendoEvidencias = false;
  evidenciaPorEliminar: EvidenciaResumen | null = null;
  eliminandoEvidencia = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quejaService: QuejaService,
    private catalogoService: CatalogoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    if (!this.folio) return;

    // "Mis Quejas" manda ?editar=1 al pulsar el lápiz, para abrir directo en modo edición.
    const abrirEnEdicion = this.route.snapshot.queryParamMap.get('editar') === '1';

    this.quejaService.miQuejaPorFolio(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        if (abrirEnEdicion && this.puedeEditar) {
          this.iniciarEdicion();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudo cargar el detalle de la queja.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });

    this.cargarEvidencias();

    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  ngOnDestroy(): void {
    // Los object URL de las miniaturas viven hasta que se revocan, aunque la pantalla ya no
    // exista.
    this.evidencias.forEach((e) => this.liberarUrl(e));
  }

  // =========================================================================================
  // Estado del trámite
  // =========================================================================================

  get estatus(): string {
    return etiquetaEstatus(this.queja?.estatus);
  }

  get claseEstatus(): string {
    return claseEstatus(this.queja?.estatus);
  }

  get pasos(): PasoTramite[] {
    return pasosDelTramite(this.queja?.estatus);
  }

  get puedeEditar(): boolean {
    return esEditable(this.queja?.estatus);
  }

  get fueRechazada(): boolean {
    return this.queja?.estatus === 'RECHAZADA';
  }

  /** La queja fue rechazada y todavía se puede corregir y reenviar a Recepción. */
  get puedeCorregir(): boolean {
    return puedeCorregirse(this.queja?.estatus);
  }

  // =========================================================================================
  // Datos para mostrar
  // =========================================================================================

  nombreUnidad(clave?: string): string {
    if (!clave) return '—';
    return this.dependencias.find((d) => d.clave === clave)?.nombre ?? clave;
  }

  /** Nombre completo del quejoso — antes esta pantalla ni siquiera lo mostraba. */
  get nombreQuejoso(): string {
    const q = this.queja;
    if (!q) return '—';
    const partes = [q.nombreQuejoso, q.apellido1Quejoso, q.apellido2Quejoso];
    return partes.filter(Boolean).join(' ') || '—';
  }

  /** Nombre del denunciado, ahora incluyendo el segundo apellido. */
  get nombreDenunciado(): string {
    const q = this.queja;
    if (!q) return '—';
    const partes = [q.nombreDenunciado, q.apellido1Denunciado, q.apellido2Denunciado];
    return partes.filter(Boolean).join(' ') || 'No se especificó';
  }

  get identificacionQuejoso(): string {
    const q = this.queja;
    if (!q?.numeroIdentificacionQuejoso) return '—';
    const etiqueta =
      q.tipoIdentificacionQuejoso === 'empleado' ? 'Núm. de empleado' : 'Núm. de boleta';
    return `${etiqueta}: ${q.numeroIdentificacionQuejoso}`;
  }

  formatearTamanio(bytes?: number): string {
    return bytes ? Reglas.formatearTamanio(bytes) : '';
  }

  extension(nombre: string): string {
    return (nombre.split('.').pop() ?? '').toUpperCase() || 'ARCHIVO';
  }

  /** Quita el prefijo interno para que el usuario vea el nombre con el que subió el archivo. */
  nombreVisible(nombre: string): string {
    return nombre.startsWith(Reglas.PREFIJO_IDENTIFICACION)
      ? nombre.slice(Reglas.PREFIJO_IDENTIFICACION.length)
      : nombre;
  }

  esIdentificacion(evidencia: EvidenciaResumen): boolean {
    return (
      evidencia.tipo === 'IDENTIFICACION' ||
      evidencia.nombreArchivo.startsWith(Reglas.PREFIJO_IDENTIFICACION)
    );
  }

  // =========================================================================================
  // Evidencias
  // =========================================================================================

  private cargarEvidencias(): void {
    this.quejaService.misEvidencias(this.folio).subscribe({
      next: (evidencias) => {
        this.evidencias.forEach((e) => this.liberarUrl(e));
        this.evidencias = evidencias.map((evidencia) => ({
          evidencia,
          url: null,
          cargando: false,
        }));
        this.evidencias.forEach((item) => this.cargarMiniatura(item));
        this.cdr.detectChanges();
      },
      error: () => {
        // No es crítico para ver el detalle si esto falla — simplemente no se muestran.
      },
    });
  }

  /**
   * Descarga la imagen para poder mostrarla.
   *
   * No se puede poner la URL del endpoint directamente en un `<img src>`: ese endpoint exige
   * el JWT en la cabecera Authorization y una etiqueta `<img>` no manda cabeceras. Se pide el
   * archivo con HttpClient (que sí pasa por el interceptor del token) y con el blob se arma
   * un object URL local.
   */
  private cargarMiniatura(item: EvidenciaConVista): void {
    const mime = item.evidencia.tipoMime ?? '';
    if (!mime.startsWith('image/')) return;

    item.cargando = true;
    this.quejaService.contenidoEvidencia(this.folio, item.evidencia.id).subscribe({
      next: (blob) => {
        item.url = URL.createObjectURL(blob);
        item.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        item.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  private liberarUrl(item: EvidenciaConVista): void {
    if (item.url) {
      URL.revokeObjectURL(item.url);
      item.url = null;
    }
  }

  /** Abre el archivo en una pestaña nueva — sirve para PDF, video y audio. */
  abrirEvidencia(item: EvidenciaConVista): void {
    this.quejaService.contenidoEvidencia(this.folio, item.evidencia.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const ventana = window.open(url, '_blank');
        if (!ventana) {
          this.toast.advertencia('Tu navegador bloqueó la ventana. Permite las ventanas emergentes.');
        }
        // Se revoca con retraso: si se revoca de inmediato, la pestaña nueva puede no haber
        // alcanzado a leer el blob.
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: () => this.toast.error('No se pudo abrir el archivo.'),
    });
  }

  onArchivosSeleccionados(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.subirEvidencias(Array.from(input.files));
    input.value = '';
  }

  private subirEvidencias(archivos: File[]): void {
    const validos: File[] = [];

    for (const archivo of archivos) {
      const extension = '.' + (archivo.name.split('.').pop()?.toLowerCase() ?? '');
      if (!Reglas.EVIDENCIA_EXTENSIONES.includes(extension)) {
        this.toast.error(`"${archivo.name}" no es un tipo permitido. Usa PDF, JPG, PNG, MP4 o MP3.`);
        continue;
      }
      if (archivo.size > Reglas.EVIDENCIA_TAMANIO_MAXIMO) {
        this.toast.error(
          `"${archivo.name}" pesa ${Reglas.formatearTamanio(archivo.size)}; el máximo es 30MB.`,
        );
        continue;
      }
      validos.push(archivo);
    }

    if (validos.length === 0) return;

    this.subiendoEvidencias = true;
    this.quejaService.agregarEvidencias(this.folio, validos).subscribe({
      next: (evidencias) => {
        this.subiendoEvidencias = false;
        this.evidencias.forEach((e) => this.liberarUrl(e));
        this.evidencias = evidencias.map((evidencia) => ({ evidencia, url: null, cargando: false }));
        this.evidencias.forEach((item) => this.cargarMiniatura(item));
        this.toast.exito(
          validos.length === 1 ? 'Evidencia agregada.' : `${validos.length} evidencias agregadas.`,
        );
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.subiendoEvidencias = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudieron agregar las evidencias.');
        this.cdr.detectChanges();
      },
    });
  }

  pedirConfirmacionEvidencia(item: EvidenciaConVista): void {
    this.evidenciaPorEliminar = item.evidencia;
  }

  cerrarConfirmacionEvidencia(): void {
    if (this.eliminandoEvidencia) return;
    this.evidenciaPorEliminar = null;
  }

  confirmarEliminarEvidencia(): void {
    const evidencia = this.evidenciaPorEliminar;
    if (!evidencia) return;

    this.eliminandoEvidencia = true;
    this.quejaService.eliminarEvidencia(this.folio, evidencia.id).subscribe({
      next: () => {
        this.eliminandoEvidencia = false;
        this.evidenciaPorEliminar = null;
        const quitado = this.evidencias.find((e) => e.evidencia.id === evidencia.id);
        if (quitado) this.liberarUrl(quitado);
        this.evidencias = this.evidencias.filter((e) => e.evidencia.id !== evidencia.id);
        this.toast.exito('Evidencia eliminada.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.eliminandoEvidencia = false;
        this.evidenciaPorEliminar = null;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo eliminar la evidencia.');
        this.cdr.detectChanges();
      },
    });
  }

  // =========================================================================================
  // Edición
  // =========================================================================================

  iniciarEdicion(): void {
    if (!this.queja) return;
    this.formDescripcion = this.queja.descripcion ?? '';
    this.formUnidadAcademica = this.queja.unidadAcademicaClave ?? '';
    this.formFechaHechos = this.queja.fechaHechos ?? '';
    this.formNombreDenunciado = this.queja.nombreDenunciado ?? '';
    this.formApellido1Denunciado = this.queja.apellido1Denunciado ?? '';
    this.formApellido2Denunciado = this.queja.apellido2Denunciado ?? '';
    this.editando = true;
  }

  cancelarEdicion(): void {
    this.editando = false;
    // Se limpia el ?editar=1 para que recargar la página no vuelva a abrir el formulario.
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  soloLetras(event: KeyboardEvent): void {
    if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.metaKey &&
      !/[A-Za-zÁÉÍÓÚÜÑáéíóúüñ '\-]/.test(event.key)
    ) {
      event.preventDefault();
    }
  }

  /** Mismas reglas que el formulario público — el backend las aplica igual en la edición. */
  private get errorEdicion(): string | null {
    const descripcion = this.formDescripcion.trim();
    if (!descripcion) return 'La descripción de los hechos no puede quedar vacía.';
    if (descripcion.length < Reglas.DESCRIPCION_LONGITUD_MINIMA) {
      return `Describe los hechos con al menos ${Reglas.DESCRIPCION_LONGITUD_MINIMA} caracteres.`;
    }
    if (this.formFechaHechos && !Reglas.esFechaValida(this.formFechaHechos)) {
      return 'La fecha de los hechos no es una fecha válida.';
    }
    return (
      Reglas.validarNombre(this.formNombreDenunciado, 'el nombre del denunciado', false) ??
      Reglas.validarNombre(this.formApellido1Denunciado, 'el primer apellido del denunciado', false) ??
      Reglas.validarNombre(
        this.formApellido2Denunciado,
        'el segundo apellido del denunciado',
        false,
      )
    );
  }

  /**
   * Reenvía la queja a Recepción con las observaciones atendidas. Usa el mismo formulario
   * que la edición normal; lo que cambia es el endpoint y que aplica sobre una RECHAZADA.
   */
  corregirYReenviar(): void {
    const error = this.errorEdicion;
    if (error) {
      this.toast.advertencia(error);
      return;
    }

    this.guardando = true;
    this.quejaService
      .corregirMiQueja(this.folio, {
        descripcion: this.formDescripcion.trim(),
        unidadAcademicaClave: this.formUnidadAcademica || undefined,
        fechaHechos: this.formFechaHechos || undefined,
        nombreDenunciado: this.formNombreDenunciado.trim() || undefined,
        apellido1Denunciado: this.formApellido1Denunciado.trim() || undefined,
        apellido2Denunciado: this.formApellido2Denunciado.trim() || undefined,
      })
      .subscribe({
        next: (quejaActualizada) => {
          this.guardando = false;
          this.queja = quejaActualizada;
          this.editando = false;
          this.toast.exito('Tu queja se reenvió a la Defensoría para una nueva revisión.');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.guardando = false;
          this.toast.error(err?.error?.mensaje ?? 'No se pudo reenviar la queja. Intenta de nuevo.');
          this.cdr.detectChanges();
        },
      });
  }

  guardarEdicion(): void {
    const error = this.errorEdicion;
    if (error) {
      this.toast.advertencia(error);
      return;
    }

    this.guardando = true;
    this.quejaService
      .editarMiQueja(this.folio, {
        descripcion: this.formDescripcion.trim(),
        unidadAcademicaClave: this.formUnidadAcademica || undefined,
        fechaHechos: this.formFechaHechos || undefined,
        nombreDenunciado: this.formNombreDenunciado.trim() || undefined,
        apellido1Denunciado: this.formApellido1Denunciado.trim() || undefined,
        apellido2Denunciado: this.formApellido2Denunciado.trim() || undefined,
      })
      .subscribe({
        next: (quejaActualizada) => {
          this.guardando = false;
          this.queja = quejaActualizada;
          this.editando = false;
          this.toast.exito('Los cambios de tu queja se guardaron correctamente.');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.guardando = false;
          this.toast.error(err?.error?.mensaje ?? 'No se pudo guardar la edición. Intenta de nuevo.');
          this.cdr.detectChanges();
        },
      });
  }
}
