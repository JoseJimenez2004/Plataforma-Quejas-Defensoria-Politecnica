import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ExpedienteDetalle, EvidenciaDetalle } from '../../core/models/expediente-detalle';
import { NotaAnalisis } from '../../core/models/nota-analisis.model';
import { AcuerdoConciliacion } from '../../core/models/conciliacion.model';
import { CitaPrimerContacto, citaCerrada } from '../../core/models/cita-primer-contacto';
import { ExpedienteService } from '../../core/services/expediente.service';
import { NotaAnalisisService } from '../../core/services/nota-analisis.service';
import { ConciliacionService } from '../../core/services/conciliacion.service';
import { DictamenService } from '../../core/services/dictamen.service';
import { AgendaService } from '../../core/services/agenda.service';
import { AntecedentesService } from '../../core/services/antecedentes.service';
import { AnalistaSesionService } from '../../core/services/analista-sesion.service';
import {
  claseEstatus,
  estaAbierto,
  formatearFechaHora
} from '../../core/utils/estatus-expediente';
import { abrirBlob, mensajeDeError } from '../../core/utils/archivos';
import { DenunciadoService } from '../../core/services/denunciado.service';
import { RespuestaDenunciado, SolicitudDenunciado } from '../../core/models/denunciado.model';
import {
  SolicitudDenunciadoDialog,
  SolicitudDenunciadoDialogData
} from '../../shared/solicitud-denunciado-dialog/solicitud-denunciado-dialog';
import {
  RespuestaDenunciadoDialog,
  RespuestaDenunciadoDialogData
} from '../../shared/respuesta-denunciado-dialog/respuesta-denunciado-dialog';
import {
  AntecedenteDetalleDialog,
  AntecedenteDetalleData
} from '../../shared/antecedente-detalle-dialog/antecedente-detalle-dialog';
import { NuevaCita } from '../../shared/nueva-cita/nueva-cita';
import { Antecedente } from '../../core/models/antecedentes.model';
import { ArchivoRespuestaDenunciado } from '../../core/models/denunciado.model';
import { ResumenModelo, ResumenService } from '../../core/services/resumen.service';
import { AbogadoSubdefensoria, AbogadosService } from '../../core/services/abogados.service';
import { generarResumenSimulado } from '../../core/utils/resumen-simulado';
import { etiquetaCategoria } from '../../core/models/cita-primer-contacto';
import { descargarBlob } from '../../core/utils/archivos';

/** Pestañas del expediente (diseño 2026-10-18). Viven en la URL: ?tab=ia|expediente|gestion|resolucion. */
export type PestanaExpediente = 'ia' | 'expediente' | 'gestion' | 'resolucion';

/** Evento del "Historial del expediente". */
export interface EventoHistorial {
  fecha: string;
  titulo: string;
  detalle: string;
  tipo: 'inicio' | 'analisis' | 'normal';
}

/** Etiqueta de cada fragmento clave de la queja, según de qué habla. */
function etiquetaFragmento(texto: string, indice: number): string {
  const t = texto.toLowerCase();
  if (indice === 0) return 'Inicio del contexto';
  if (/(dijo|respond|grit|insult|coment|molest|trato|humill|burl)/.test(t)) return 'Trato recibido';
  if (/(reprob|calific|baja|sanci|afect|perd|consecuen)/.test(t)) return 'Consecuencia';
  if (/(problema|situaci|injust|suced|hecho|ocurri)/.test(t)) return 'Hechos principales';
  return 'Descripción de la situación';
}

@Component({
  selector: 'app-expediente',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule,
    NuevaCita,
    NgTemplateOutlet,
    DecimalPipe
  ],
  templateUrl: './expediente.html',
  styleUrl: './expediente.css'
})
export class Expediente implements OnInit, OnDestroy {
  folio = '';

  /** Pestaña visible; por defecto "Expediente", como en el diseño. */
  pestana: PestanaExpediente = 'expediente';
  narrativaCompleta = false;
  mostrarNuevoOficio = false;
  mostrarNuevaCita = false;

  // ---- Análisis IA
  resumen: ResumenModelo | null = null;
  resumenSimulado = false;
  cargandoResumen = false;
  fechaResumen = '';
  antecedentesLista: Antecedente[] = [];
  private peticionResumen?: Subscription;

  // ---- Resolución
  decision: 'competente' | 'improcedente' | null = null;
  justificacion = '';
  observacionesDictamen = '';
  responsableTurno = '';
  abogados: AbogadoSubdefensoria[] = [];
  readonly etiquetaCategoria = etiquetaCategoria;

  // ---- Oficios (la nota de análisis es el oficio del expediente)
  nuevoOficioAsunto = '';
  nuevoOficioTexto = '';
  guardandoOficio = false;

  /** Oficio que se está editando en línea (solo los propios). */
  notaEnEdicion: number | null = null;
  asuntoEdicion = '';
  textoEdicion = '';

  expediente: ExpedienteDetalle = {
    folio: '',
    asunto: '',
    fechaIngreso: '',
    estatus: '',
    estatusCodigo: '',
    prioridad: 'Media',
    narrativa: '',
    quejoso: {
      nombre: '',
      boleta: '',
      correo: '',
      telefono: '',
      unidadAcademica: ''
    },
    evidencias: [],
    notas: []
  };

  acuerdos: AcuerdoConciliacion[] = [];
  citaActiva?: CitaPrimerContacto;
  /** Todas las citas del expediente (incluidas las canceladas y reagendadas), más recientes primero. */
  historialCitas: CitaPrimerContacto[] = [];

  // Versión del denunciado: correos enviados (Primer Contacto) y respuestas (denunciado-service).
  solicitudesDenunciado: SolicitudDenunciado[] = [];
  respuestasDenunciado: RespuestaDenunciado[] = [];
  enviandoSolicitudDenunciado = false;

  /** Resumen de la búsqueda automática de antecedentes al abrir el expediente. */
  antecedentes?: { total: number; mismoQuejoso: number; maxSimilitud: number };

  mostrarFormConciliacion = false;
  conciliacion = { asunto: '', terminos: '' };

  analistaActualId?: number;

  cargando = false;
  sinDatos = false;
  procesando = false;

  readonly claseEstatus = claseEstatus;
  readonly formatearFechaHora = formatearFechaHora;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private expedienteService: ExpedienteService,
    private notaAnalisisService: NotaAnalisisService,
    private conciliacionService: ConciliacionService,
    private dictamenService: DictamenService,
    private agendaService: AgendaService,
    private antecedentesService: AntecedentesService,
    private analistaSesion: AnalistaSesionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private denunciadoService: DenunciadoService,
    private dialog: MatDialog,
    private resumenService: ResumenService,
    private abogadosService: AbogadosService
  ) {}

  ngOnInit(): void {
    this.analistaSesion.obtener().subscribe({
      next: (analista) => {
        this.analistaActualId = analista.id;
        this.cdr.detectChanges();
      }
    });

    this.route.paramMap.subscribe(params => {
      this.folio = params.get('id') ?? '';

      if (this.folio) {
        this.cargarExpediente();
      }
    });

    this.route.queryParamMap.subscribe(params => {
      const tab = params.get('tab') as PestanaExpediente | null;
      this.pestana = tab && ['ia', 'expediente', 'gestion', 'resolucion'].includes(tab) ? tab : 'expediente';
      const decision = params.get('decision');
      if (decision === 'competente' || decision === 'improcedente') {
        this.decision = decision;
      }
      this.cdr.detectChanges();
    });

    this.abogadosService.listar().subscribe({
      next: (lista) => {
        this.abogados = lista;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.peticionResumen?.unsubscribe();
  }

  /** Cambia de pestaña sin recargar el expediente (queda en la URL para "atrás"). */
  irAPestana(p: PestanaExpediente): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { tab: p === 'expediente' ? null : p }, queryParamsHandling: 'merge', replaceUrl: false });
  }

  volver(): void {
    this.router.navigate(['/bandeja']);
  }

  get abierto(): boolean {
    return estaAbierto(this.expediente.estatusCodigo);
  }

  get conciliacionPendiente(): boolean {
    return this.acuerdos.some(a => a.estado === 'PENDIENTE');
  }

  /**
   * CU-PC-03: abrir el expediente lo pasa de TURNADA a EN_ANALISIS (el backend registra
   * quién y cuándo). Por eso aquí se usa iniciarAnalisis y no una consulta simple.
   */
  cargarExpediente(): void {
    this.cargando = true;
    this.sinDatos = false;

    this.expedienteService.iniciarAnalisis(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cargando = false;
        this.cdr.detectChanges();

        this.cargarConciliaciones();
        this.cargarCitaActiva();
        this.cargarResumenAntecedentes();
        this.cargarDenunciado();
        this.generarResumen();
      },
      error: (error) => {
        this.cargando = false;

        if (error.status === 404) {
          this.sinDatos = true;
          this.cdr.detectChanges();
          this.snackBar.open(
            `No se ha recibido de Revisión ningún expediente con folio ${this.folio}.`,
            'Cerrar',
            { duration: 4000 }
          );
          return;
        }

        this.cdr.detectChanges();
        this.snackBar.open(
          'No fue posible cargar el expediente. Intenta nuevamente o contacta al administrador del sistema.',
          'Cerrar',
          { duration: 4000 }
        );
      }
    });
  }

  /** Recarga sin volver a "abrir" (solo consulta). */
  private recargarExpediente(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cdr.detectChanges();
      }
    });
  }

  // ---------------------------------------------------------------- Evidencias (CU-06/07)

  abrirEvidencia(evidencia: EvidenciaDetalle): void {
    this.expedienteService.descargarEvidencia(evidencia.id).subscribe({
      next: (archivo) => abrirBlob(archivo),
      error: (error) => {
        this.snackBar.open(
          mensajeDeError(error, 'No fue posible abrir la evidencia.'),
          'Cerrar',
          { duration: 3500 }
        );
      }
    });
  }

  // ---------------------------------------------------------------- Oficios (CU-05)

  esNotaPropia(nota: NotaAnalisis): boolean {
    return this.analistaActualId !== undefined && nota.analistaId === this.analistaActualId;
  }

  cancelarNuevoOficio(): void {
    this.mostrarNuevoOficio = false;
    this.nuevoOficioAsunto = '';
    this.nuevoOficioTexto = '';
  }

  /** Guarda el oficio y abre su PDF. */
  agregarOficio(): void {
    const asunto = this.nuevoOficioAsunto.trim();
    const contenido = this.nuevoOficioTexto.trim();

    if (!asunto || !contenido || this.guardandoOficio) return;

    if (!this.expediente.folio) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    this.guardandoOficio = true;
    this.notaAnalisisService.crearNota({
      folio: this.expediente.folio,
      asunto,
      contenido
    }).subscribe({
      next: (oficio) => {
        this.expediente.notas = [oficio, ...this.expediente.notas];
        this.guardandoOficio = false;
        this.cancelarNuevoOficio();
        this.cdr.detectChanges();
        this.snackBar.open(`Oficio ${oficio.numeroOficio} guardado.`, 'Cerrar', { duration: 3000 });
        this.verOficio(oficio);
      },
      error: (error) => {
        this.guardandoOficio = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible guardar el oficio.'), 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  verOficio(nota: NotaAnalisis): void {
    this.notaAnalisisService.obtenerPdf(nota.id).subscribe({
      next: (pdf) => abrirBlob(pdf),
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible generar el PDF del oficio.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  editarNota(nota: NotaAnalisis): void {
    this.notaEnEdicion = nota.id;
    this.asuntoEdicion = nota.asunto ?? '';
    this.textoEdicion = nota.contenido;
  }

  cancelarEdicion(): void {
    this.notaEnEdicion = null;
    this.asuntoEdicion = '';
    this.textoEdicion = '';
  }

  guardarEdicion(nota: NotaAnalisis): void {
    const asunto = this.asuntoEdicion.trim();
    const contenido = this.textoEdicion.trim();
    if (!asunto || !contenido) return;

    this.notaAnalisisService.actualizarNota(nota.id, {
      folio: this.expediente.folio,
      asunto,
      contenido
    }).subscribe({
      next: (actualizada) => {
        this.expediente.notas = this.expediente.notas.map(n => n.id === actualizada.id ? actualizada : n);
        this.cancelarEdicion();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible editar el oficio.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  eliminarNota(nota: NotaAnalisis): void {
    if (!confirm(`¿Eliminar el oficio ${nota.numeroOficio}? Esta acción no se puede deshacer.`)) return;

    this.notaAnalisisService.eliminarNota(nota.id).subscribe({
      next: () => {
        this.expediente.notas = this.expediente.notas.filter(n => n.id !== nota.id);
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible eliminar el oficio.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  // ---------------------------------------------------------------- Conciliación (CU-10)

  cargarConciliaciones(): void {
    const estabaAbierto = this.abierto;

    this.conciliacionService.listar(this.folio).subscribe({
      next: (acuerdos) => {
        this.acuerdos = acuerdos;
        this.cdr.detectChanges();

        // Si el quejoso ya aceptó, el backend acaba de turnar el expediente.
        if (estabaAbierto && acuerdos.some(a => a.estado === 'ACEPTADO')) {
          this.recargarExpediente();
        }
      }
    });
  }

  proponerConciliacion(): void {
    const asunto = this.conciliacion.asunto.trim();
    const terminos = this.conciliacion.terminos.trim();

    if (!asunto || !terminos) {
      this.snackBar.open('Escribe el asunto y los términos del acuerdo.', 'Cerrar', { duration: 3000 });
      return;
    }

    if (!confirm('El acuerdo se enviará al quejoso para que lo acepte o rechace desde su panel. ¿Continuar?')) {
      return;
    }

    this.procesando = true;

    this.conciliacionService.proponer({ folio: this.folio, asunto, terminos }).subscribe({
      next: (acuerdo) => {
        this.acuerdos = [acuerdo, ...this.acuerdos];
        this.conciliacion = { asunto: '', terminos: '' };
        this.mostrarFormConciliacion = false;
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open('Acuerdo enviado al quejoso.', 'Cerrar', { duration: 3000 });
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible proponer el acuerdo.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }

  etiquetaAcuerdo(estado: AcuerdoConciliacion['estado']): string {
    switch (estado) {
      case 'ACEPTADO':
        return 'Aceptado por el quejoso';
      case 'RECHAZADO':
        return 'Rechazado por el quejoso';
      default:
        return 'Esperando respuesta';
    }
  }

  // ---------------------------------------------------------------- Antecedentes

  /** Al abrir el expediente se busca en automático; el detalle está en su propia pantalla. */
  private cargarResumenAntecedentes(): void {
    this.antecedentesService.buscar(this.folio).subscribe({
      next: (busqueda) => {
        // Todas las listas del modelo (involucrados, hechos, coincidencias fuertes) sin repetir folios.
        const vistos = new Set<string>();
        this.antecedentesLista = [
          ...(busqueda.coincidenciasFuertes ?? []),
          ...busqueda.resultados,
          ...(busqueda.porHechos ?? [])
        ].filter(a => !vistos.has(a.folioQueja) && !!vistos.add(a.folioQueja))
          .sort((a, b) => b.similitud - a.similitud);
        this.antecedentes = {
          total: this.antecedentesLista.length,
          mismoQuejoso: this.antecedentesLista.filter(a => a.mismoQuejoso).length,
          maxSimilitud: Math.round(Math.max(0, ...this.antecedentesLista.map(a => a.similitud)))
        };
        this.cdr.detectChanges();
      }
    });
  }

  buscarAntecedentes(): void {
    this.router.navigate(['/expediente', this.folio, 'antecedentes']);
  }

  verAntecedente(a: Antecedente): void {
    const data: AntecedenteDetalleData = { ...a };
    this.dialog.open(AntecedenteDetalleDialog, { width: '720px', data });
  }

  /** Color de la similitud: alta (guinda), media (rosa) o baja (lila). */
  claseSimilitud(similitud: number): string {
    return similitud >= 50 ? 'alta' : similitud >= 40 ? 'media' : 'baja';
  }

  // ---------------------------------------------------------------- Análisis IA (resumen)

  /**
   * Borrador del resumen con el modelo (resumen-service). Puede tardar hasta ~2 minutos, por
   * eso se pide al abrir el expediente; si el modelo no responde se usa el resumen simulado
   * del navegador y la pantalla lo dice.
   */
  private generarResumen(): void {
    const texto = this.expediente.narrativa?.trim();
    if (!texto || this.resumen || this.cargandoResumen) return;
    this.cargandoResumen = true;
    this.peticionResumen = this.resumenService.resumir(texto).subscribe({
      next: (r) => {
        this.resumen = r;
        this.resumenSimulado = false;
        this.cargandoResumen = false;
        this.fechaResumen = new Date().toISOString().slice(0, 16);
        this.cdr.detectChanges();
      },
      error: () => {
        const s = generarResumenSimulado(texto);
        const oraciones = texto.split(/(?<=[.!?])\s+/).filter(o => o.trim().length > 25);
        this.resumen = {
          resumen: s.resumen,
          fragmentos: (s.frasesImpacto.length >= 3 ? s.frasesImpacto : oraciones.slice(0, 5)),
          requiereRevision: true
        };
        this.resumenSimulado = true;
        this.cargandoResumen = false;
        this.fechaResumen = new Date().toISOString().slice(0, 16);
        this.cdr.detectChanges();
      }
    });
  }

  get fragmentos(): { texto: string; etiqueta: string }[] {
    return (this.resumen?.fragmentos ?? []).map((t, i) => ({ texto: t, etiqueta: etiquetaFragmento(t, i) }));
  }

  copiarResumen(): void {
    if (!this.resumen?.resumen) return;
    navigator.clipboard?.writeText(this.resumen.resumen).then(
      () => this.snackBar.open('Resumen copiado.', 'Cerrar', { duration: 2000 }),
      () => this.snackBar.open('No se pudo copiar el resumen.', 'Cerrar', { duration: 2500 })
    );
  }

  verNarrativaCompleta(): void {
    this.narrativaCompleta = true;
    this.irAPestana('expediente');
  }

  // ---------------------------------------------------------------- Historial

  /** Línea de tiempo con lo que ya se cargó del expediente (sin peticiones extra). */
  get historial(): EventoHistorial[] {
    const e = this.expediente;
    const eventos: EventoHistorial[] = [];
    const registro = e.datosQueja?.fechaRegistro;
    if (registro) {
      eventos.push({ fecha: registro, titulo: 'Solicitud recibida', detalle: 'Solicitud enviada por el quejoso a través del portal institucional.', tipo: 'inicio' });
    }
    if (e.fechaInicioAnalisis) {
      eventos.push({ fecha: e.fechaInicioAnalisis, titulo: 'En análisis', detalle: 'Asignado a ' + (e.analistaAnalisisNombre || 'Primer Contacto') + '.', tipo: 'analisis' });
    }
    for (const n of e.notas) {
      eventos.push({ fecha: n.fechaCreacion, titulo: 'Oficio ' + n.numeroOficio, detalle: n.analistaNombre + ': ' + (n.asunto ? n.asunto + ' — ' : '') + recortar(n.contenido, 90), tipo: 'normal' });
    }
    for (const c of this.historialCitas) {
      if (c.fechaCreacion) {
        eventos.push({ fecha: c.fechaCreacion, titulo: 'Cita ' + c.estatus.toLowerCase(), detalle: `${etiquetaCategoria(c.categoria)} el ${c.fecha} a las ${c.hora} h (${c.tipo.toLowerCase()}).`, tipo: 'normal' });
      }
      if (c.fechaRespuestaQuejoso) {
        eventos.push({ fecha: c.fechaRespuestaQuejoso, titulo: 'Respuesta del quejoso', detalle: c.estatusCodigo === 'CONFIRMADA' ? 'Confirmó su cita.' : 'Canceló su cita' + (c.motivoCancelacionQuejoso ? ': ' + c.motivoCancelacionQuejoso : '.'), tipo: 'normal' });
      }
    }
    for (const s of this.solicitudesDenunciado) {
      eventos.push({ fecha: s.fechaEnvio, titulo: 'Solicitud al denunciado', detalle: 'Se pidió su versión de los hechos a ' + s.correoDenunciado + '.', tipo: 'normal' });
    }
    for (const r of this.respuestasDenunciado) {
      eventos.push({ fecha: r.fechaRegistro, titulo: 'Respuesta del denunciado', detalle: 'Registró su versión (' + r.folioRespuesta + ').', tipo: 'normal' });
    }
    for (const a of this.acuerdos) {
      if (a.fechaEmision) eventos.push({ fecha: a.fechaEmision, titulo: 'Propuesta de conciliación', detalle: a.asunto, tipo: 'normal' });
      if (a.fechaRespuesta) eventos.push({ fecha: a.fechaRespuesta, titulo: 'Conciliación ' + (a.estado === 'ACEPTADO' ? 'aceptada' : 'rechazada'), detalle: a.comentarioQuejoso || 'Respuesta del quejoso.', tipo: 'normal' });
    }
    return eventos.sort((a, b) => (a.fecha ?? '').localeCompare(b.fecha ?? ''));
  }

  // ---------------------------------------------------------------- Evidencias

  descargarEvidencia(evidencia: EvidenciaDetalle): void {
    this.expedienteService.descargarEvidencia(evidencia.id).subscribe({
      next: (archivo) => descargarBlob(archivo, evidencia.nombre),
      error: (error) => this.snackBar.open(mensajeDeError(error, 'No fue posible descargar la evidencia.'), 'Cerrar', { duration: 3500 })
    });
  }

  abrirArchivoDenunciado(a: ArchivoRespuestaDenunciado, descargar = false): void {
    this.denunciadoService.descargarArchivo(a.id).subscribe({
      next: (blob) => (descargar ? descargarBlob(blob, a.nombreArchivo) : abrirBlob(blob)),
      error: (error) => this.snackBar.open(mensajeDeError(error, 'No fue posible abrir el archivo.'), 'Cerrar', { duration: 3500 })
    });
  }

  tamanio(bytes?: number): string {
    if (!bytes) return '';
    return bytes > 1024 * 1024 ? (bytes / 1024 / 1024).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
  }

  // ---------------------------------------------------------------- Versión del denunciado

  /** Última solicitud enviada (la vigente); las anteriores quedan como historial. */
  get ultimaSolicitudDenunciado(): SolicitudDenunciado | undefined {
    return this.solicitudesDenunciado[0];
  }

  private cargarDenunciado(): void {
    this.denunciadoService.listarSolicitudes(this.folio).subscribe({
      next: (solicitudes) => {
        this.solicitudesDenunciado = solicitudes;
        this.cdr.detectChanges();
      }
    });

    const folioQueja = this.expediente?.folioOrigen;
    if (!folioQueja) return;
    this.denunciadoService.listarRespuestas(folioQueja).subscribe({
      next: (respuestas) => {
        this.respuestasDenunciado = [...respuestas].sort(
          (a, b) => (b.fechaRegistro ?? '').localeCompare(a.fechaRegistro ?? '')
        );
        this.cdr.detectChanges();
      }
    });
  }

  solicitarRespuestaDenunciado(): void {
    const folioQueja = this.expediente?.folioOrigen;
    if (!folioQueja) return;

    const data: SolicitudDenunciadoDialogData = {
      folioQueja,
      denunciado: this.expediente?.datosQueja?.denunciado,
      correoAnterior: this.ultimaSolicitudDenunciado?.correoDenunciado
    };

    this.dialog.open(SolicitudDenunciadoDialog, { width: '560px', data }).afterClosed().subscribe(correo => {
      if (!correo) return;

      this.enviandoSolicitudDenunciado = true;
      this.denunciadoService.enviarSolicitud(this.folio, correo).subscribe({
        next: () => {
          this.enviandoSolicitudDenunciado = false;
          this.snackBar.open(`Se envió la solicitud a ${correo}.`, 'Cerrar', { duration: 3500 });
          this.cargarDenunciado();
        },
        error: (error) => {
          this.enviandoSolicitudDenunciado = false;
          this.snackBar.open(mensajeDeError(error, 'No fue posible enviar la solicitud.'), 'Cerrar', {
            duration: 4500
          });
          this.cargarDenunciado();
        }
      });
    });
  }

  verRespuestaDenunciado(): void {
    const folioQueja = this.expediente?.folioOrigen;
    if (!folioQueja || this.respuestasDenunciado.length === 0) return;

    const data: RespuestaDenunciadoDialogData = { folioQueja, respuestas: this.respuestasDenunciado };
    this.dialog.open(RespuestaDenunciadoDialog, { width: '720px', data });
  }

  // ---------------------------------------------------------------- Citas (CU-04)

  private cargarCitaActiva(): void {
    this.agendaService.listarPorFolio(this.folio).subscribe({
      next: (citas) => {
        this.citaActiva = citas.find(c => !citaCerrada(c));
        this.historialCitas = citas;
        this.cdr.detectChanges();
      }
    });
  }

  agendarCita(): void {
    this.mostrarNuevaCita = true;
  }

  citaGuardada(): void {
    this.mostrarNuevaCita = false;
    this.cargarCitaActiva();
  }

  irAAgenda(): void {
    this.router.navigate(['/agenda']);
  }

  // ---------------------------------------------------------------- Dictamen / remisión

  marcarCompetente(): void {
    this.decision = 'competente';
    this.irAPestana('resolucion');
  }

  marcarImprocedente(): void {
    this.decision = 'improcedente';
    this.irAPestana('resolucion');
  }

  cancelarDecision(): void {
    this.decision = null;
    this.justificacion = '';
    this.observacionesDictamen = '';
    this.responsableTurno = '';
  }

  /** Registra el dictamen desde la pestaña Resolución (antes era una pantalla aparte). */
  registrarDictamen(): void {
    const justificacion = this.justificacion.trim();
    if (!justificacion) {
      this.snackBar.open('Escribe la justificación del dictamen.', 'Cerrar', { duration: 3000 });
      return;
    }

    if (this.decision === 'improcedente') {
      if (!confirm('¿Declarar improcedente este expediente? Después deberás generar la remisión a la instancia competente.')) return;
      const obs = this.observacionesDictamen.trim();
      this.procesando = true;
      this.dictamenService.registrarImprocedencia({
        folio: this.folio,
        justificacion: obs ? `${justificacion}\n\nObservaciones: ${obs}` : justificacion
      }).subscribe({
        next: () => {
          this.snackBar.open('Expediente declarado improcedente. Ahora genera la remisión.', 'Cerrar', { duration: 3500 });
          this.router.navigate(['/remision', this.folio]);
        },
        error: (error) => {
          this.procesando = false;
          this.cdr.detectChanges();
          this.snackBar.open(mensajeDeError(error, 'No fue posible registrar la improcedencia.'), 'Cerrar', { duration: 4000 });
        }
      });
      return;
    }

    if (!this.responsableTurno) {
      this.snackBar.open('Selecciona el responsable en Subdefensoría.', 'Cerrar', { duration: 3000 });
      return;
    }
    if (!confirm('¿Turnar este expediente a Subdefensoría?')) return;
    this.procesando = true;
    this.dictamenService.registrarCompetencia({
      folio: this.folio,
      justificacion,
      areaTurno: 'Subdefensoría',
      responsableTurno: this.responsableTurno,
      observaciones: this.observacionesDictamen.trim() || undefined
    }).subscribe({
      next: (dictamen) => {
        this.procesando = false;
        this.snackBar.open(
          dictamen.folioSubdefensoria
            ? `Expediente procedente y recibido por Subdefensoría (${dictamen.folioSubdefensoria}).`
            : 'El dictamen quedó registrado, pero Subdefensoría no respondió. Reintenta el envío.',
          'Cerrar', { duration: 4500 });
        this.cancelarDecision();
        this.recargarExpediente();
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible registrar el dictamen.'), 'Cerrar', { duration: 4000 });
      }
    });
  }

  verDictamen(): void {
    this.router.navigate(['/dictamen', this.expediente.folio, 'consulta']);
  }

  irARemision(): void {
    this.router.navigate(['/remision', this.expediente.folio]);
  }

  reenviarASubdefensoria(): void {
    this.procesando = true;

    this.dictamenService.reenviarASubdefensoria(this.folio).subscribe({
      next: (dictamen) => {
        this.procesando = false;
        this.expediente.folioSubdefensoria = dictamen.folioSubdefensoria;
        this.cdr.detectChanges();
        this.snackBar.open(
          `Subdefensoría recibió el expediente (${dictamen.folioSubdefensoria}).`,
          'Cerrar',
          { duration: 3500 }
        );
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible reenviar a Subdefensoría.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }
}

function recortar(texto: string, max: number): string {
  return texto.length <= max ? texto : texto.slice(0, max - 1).trimEnd() + '…';
}
