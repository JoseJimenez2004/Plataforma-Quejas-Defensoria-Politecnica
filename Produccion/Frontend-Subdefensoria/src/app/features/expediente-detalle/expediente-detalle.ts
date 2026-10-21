import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ExpedienteService } from '../../core/services/expediente.service';
import { OficioService } from '../../core/services/oficio.service';
import { AcuerdoConclusionService } from '../../core/services/acuerdo-conclusion.service';
import { RecordatorioService } from '../../core/services/recordatorio.service';
import { DocumentoService, abrirBlob } from '../../core/services/documento.service';
import { Recordatorio } from '../../core/models/recordatorio';
import { RespuestaExterna } from '../../core/models/respuesta-externa';
import { RespuestaExternaService } from '../../core/services/respuesta-externa.service';
import { Observable } from 'rxjs';
import { ConsultaPrimerContacto, PrimerContactoService } from '../../core/services/primer-contacto.service';
import { formatearFecha } from '../../core/utils/etapas';
import { ExpedienteInvestigacion } from '../../core/models/expediente-investigacion';
import { Oficio } from '../../core/models/oficio';
import { AcuerdoConclusion } from '../../core/models/acuerdo-conclusion';
import { ETIQUETAS_ESTATUS, PASOS_EXPEDIENTE, etiquetaEstatus } from '../../core/models/estatus-expediente';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';
import { RedactarOficioDialog, RedactarOficioDialogData } from '../../shared/redactar-oficio-dialog/redactar-oficio-dialog';
import { RespuestaExternaDialog, RespuestaExternaDialogData } from '../../shared/respuesta-externa-dialog/respuesta-externa-dialog';

/** Íconos de la barra de pasos (mismo orden que PASOS_EXPEDIENTE). */
const ICONOS_PASO: Record<string, string> = {
  RECIBIDO: 'inbox',
  EN_INVESTIGACION: 'search',
  EN_ESPERA_OFICIO: 'hourglass_top',
  ELABORO_ACUERDO: 'edit_document',
  PENDIENTE_CONCILIACION: 'handshake',
  PENDIENTE_CONCLUSION: 'gavel',
  CONCLUIDO: 'task_alt'
};

type Pestana = 'resumen' | 'primer-contacto' | 'investigacion' | 'acuerdos' | 'historial';
const PESTANAS: Pestana[] = ['resumen', 'primer-contacto', 'investigacion', 'acuerdos', 'historial'];

/** Recuadro "Siguiente paso": qué toca hacer según el estatus. */
interface SiguientePasoDetalle {
  texto: string;
  detalle?: string;
  boton?: string;
  accion?: () => void;
  secundario?: string;
  accionSecundaria?: () => void;
  hecho?: boolean;
}

interface EventoHistorial {
  fecha: string;
  titulo: string;
  detalle: string;
  tipo: 'sd' | 'pc' | 'ok' | 'gris';
}

/** Estatus en los que ya existe un acuerdo que mostrar. */
const CON_ACUERDO = ['EN_INVESTIGACION', 'ELABORO_ACUERDO', 'PENDIENTE_CONCILIACION', 'PENDIENTE_CONCLUSION', 'CONCLUIDO'];

@Component({
  selector: 'app-expediente-detalle',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatExpansionModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './expediente-detalle.html',
  styleUrl: './expediente-detalle.css'
})
export class ExpedienteDetalle implements OnInit {
  folio = '';
  expediente?: ExpedienteInvestigacion;
  oficios: Oficio[] = [];
  /** Recordatorios enviados de cada oficio, por id de oficio. */
  recordatorios: Record<number, Recordatorio[]> = {};
  /** Respuesta registrada de cada oficio (con su archivo, si lo tiene), por id de oficio. */
  respuestas: Record<number, RespuestaExterna | undefined> = {};
  acuerdo: AcuerdoConclusion | null = null;
  textoAcuerdo = '';
  textoConclusion = '';
  guardando = false;

  readonly pasos = PASOS_EXPEDIENTE;
  readonly etiquetas: Record<string, string> = ETIQUETAS_ESTATUS;
  readonly iconos = ICONOS_PASO;
  readonly etiquetaEstatus = etiquetaEstatus;
  readonly formatearFecha = formatearFecha;

  pestana: Pestana = 'resumen';
  hechosCompletos = false;

  /** Lo que trabajó Primer Contacto (solo lectura); null mientras carga o si falló. */
  pc: ConsultaPrimerContacto | null = null;
  pcError = '';

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private primerContactoService: PrimerContactoService,
    private expedienteService: ExpedienteService,
    private oficioService: OficioService,
    private acuerdoConclusionService: AcuerdoConclusionService,
    private recordatorioService: RecordatorioService,
    private documentoService: DocumentoService,
    private respuestaExternaService: RespuestaExternaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    const tab = this.route.snapshot.queryParamMap.get('tab') as Pestana | null;
    if (tab && PESTANAS.includes(tab)) this.pestana = tab;
  }

  cambiarPestana(pestana: Pestana): void {
    this.pestana = pestana;
    this.router.navigate([], { queryParams: { tab: pestana === 'resumen' ? null : pestana }, replaceUrl: true });
  }

  // ------------------------------------------------------------ Primer Contacto (solo lectura)

  cargarPrimerContacto(folioPc: string): void {
    if (!folioPc || this.pc) return;
    this.primerContactoService.consultar(folioPc).subscribe({
      next: (consulta) => {
        this.pc = consulta;
        this.pcError = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.pcError = err?.status === 403
          ? 'Tu cuenta no tiene permiso para consultar Primer Contacto.'
          : 'No fue posible cargar la información de Primer Contacto.';
        this.cdr.detectChanges();
      }
    });
  }

  verOficioPc(id: number): void {
    this.abrirPdf(this.primerContactoService.pdfOficio(id));
  }

  verEvidencia(id: number): void {
    this.abrirPdf(this.primerContactoService.evidencia(id));
  }

  /** Misma etiqueta de cita que en Primer Contacto. */
  etiquetaCita(estatus: string): string {
    const etiquetas: Record<string, string> = {
      PROGRAMADA: 'Programada', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada',
      CANCELADA_QUEJOSO: 'Cancelada por el quejoso', SIN_RESPUESTA: 'Sin respuesta', REAGENDADA: 'Reagendada'
    };
    return etiquetas[estatus] ?? estatus;
  }

  // ------------------------------------------------------------ Siguiente paso

  get oficioActivo(): Oficio | undefined {
    return this.oficios.find(o => this.esOficioActivo(o));
  }

  get siguientePaso(): SiguientePasoDetalle | null {
    const e = this.expediente;
    if (!e) return null;
    const activo = this.oficioActivo;

    switch (e.estatus) {
      case 'EN_INVESTIGACION':
        if (this.puedeSolicitarOficio) {
          return {
            texto: this.acuerdoRechazado
              ? 'El quejoso rechazó el acuerdo: emite un nuevo oficio de gestión con el director.'
              : 'Solicita el oficio a la unidad académica.',
            boton: 'Solicitar oficio', accion: () => this.solicitarOficio()
          };
        }
        return { texto: 'Expediente en investigación.' };
      case 'EN_ESPERA_OFICIO':
        if (activo) {
          return {
            texto: `Registra la respuesta de ${activo.destinatarioNombre} al oficio ${activo.numeroOficio}.`,
            detalle: activo.estatus === 'VENCIDO'
              ? `El plazo venció el ${formatearFecha(activo.fechaLimite)}.`
              : `Vence el ${formatearFecha(activo.fechaLimite)}.`,
            boton: 'Registrar respuesta', accion: () => this.abrirRespuestaExterna(activo),
            secundario: 'Enviar recordatorio', accionSecundaria: () => this.abrirRecordatorio(activo)
          };
        }
        return { texto: 'Esperando la respuesta de la unidad académica.' };
      case 'ELABORO_ACUERDO':
        return { texto: 'La unidad respondió. Redacta el acuerdo de conciliación para el quejoso.', boton: 'Redactar acuerdo', accion: () => this.cambiarPestana('acuerdos') };
      case 'PENDIENTE_CONCILIACION':
        return { texto: 'Acuerdo de conciliación enviado. Falta que el quejoso lo acepte o lo rechace.', detalle: 'El expediente avanza solo cuando responda.', secundario: 'Ver acuerdo (PDF)', accionSecundaria: () => this.verPdfAcuerdoConciliacion() };
      case 'PENDIENTE_CONCLUSION':
        return { texto: 'El quejoso aceptó el acuerdo de conciliación. Redacta el acuerdo de conclusión.', boton: 'Redactar conclusión', accion: () => this.cambiarPestana('acuerdos') };
      case 'CONCLUIDO':
        return { texto: 'Expediente concluido.', hecho: true, secundario: 'Ver acuerdo de conclusión (PDF)', accionSecundaria: () => this.verPdfAcuerdoConclusion() };
      default:
        return null;
    }
  }

  // ------------------------------------------------------------ Historial

  get historial(): EventoHistorial[] {
    const eventos: EventoHistorial[] = [];
    const e = this.expediente;
    if (!e) return eventos;

    const pcExp = this.pc?.expediente;
    if (pcExp?.datosQueja?.fechaRegistro) {
      eventos.push({ fecha: pcExp.datosQueja.fechaRegistro, titulo: 'Queja registrada', detalle: 'El quejoso presentó la queja.', tipo: 'gris' });
    }
    if (pcExp?.fechaInicioAnalisis) {
      eventos.push({ fecha: pcExp.fechaInicioAnalisis, titulo: 'Análisis en Primer Contacto', detalle: 'Lo tomó ' + (pcExp.analistaAnalisisNombre ?? 'Primer Contacto') + '.', tipo: 'pc' });
    }
    for (const n of pcExp?.notas ?? []) {
      eventos.push({ fecha: n.fechaCreacion, titulo: 'Oficio ' + n.numeroOficio + ' (Primer Contacto)', detalle: (n.asunto ?? 'Nota de análisis') + ' · ' + n.analistaNombre, tipo: 'pc' });
    }
    for (const c of this.pc?.citas ?? []) {
      eventos.push({ fecha: c.fechaCreacion, titulo: 'Cita con el quejoso', detalle: `${formatearFecha(c.fechaCita)} ${c.horaCita ?? ''} · ${this.etiquetaCita(c.estatus)}`, tipo: 'pc' });
    }
    if (this.pc?.dictamen) {
      eventos.push({ fecha: this.pc.dictamen.fechaDictamen, titulo: 'Dictamen de Primer Contacto', detalle: this.pc.dictamen.resultado + ' · ' + this.pc.dictamen.analistaNombre, tipo: 'pc' });
    }
    eventos.push({ fecha: e.fechaAdmision, titulo: 'Recibido en Subdefensoría', detalle: 'Expediente ' + e.folio + '.', tipo: 'sd' });

    for (const o of this.oficios) {
      eventos.push({ fecha: o.fechaEnvio, titulo: 'Oficio ' + o.numeroOficio, detalle: `${o.fase === 'GESTION_DIRECTOR' ? 'Gestión con el director' : 'Solicitud de información'} · ${o.destinatarioNombre}`, tipo: 'sd' });
      for (const rec of this.recordatorios[o.id] ?? []) {
        eventos.push({ fecha: rec.fechaEnvio, titulo: 'Recordatorio del oficio ' + o.numeroOficio, detalle: 'Nueva fecha límite: ' + formatearFecha(rec.nuevaFechaLimite), tipo: 'sd' });
      }
      const resp = this.respuestas[o.id];
      if (resp) {
        eventos.push({ fecha: resp.fechaRecepcion, titulo: 'Respuesta al oficio ' + o.numeroOficio, detalle: resp.canalRecepcion + (resp.numeroOficioRespuestaUA ? ' · Oficio ' + resp.numeroOficioRespuestaUA : ''), tipo: 'ok' });
      }
    }
    const a = this.acuerdo;
    if (a?.fechaEnvioSecretarial) {
      eventos.push({ fecha: a.fechaEnvioSecretarial, titulo: 'Acuerdo de conciliación enviado', detalle: 'Se envió al quejoso para su respuesta.', tipo: 'sd' });
    }
    if (a?.fechaRespuestaQuejoso) {
      eventos.push({ fecha: a.fechaRespuestaQuejoso, titulo: a.respuestaQuejoso === 'ACEPTADO' ? 'El quejoso aceptó el acuerdo' : 'El quejoso rechazó el acuerdo', detalle: a.comentarioQuejoso ?? '', tipo: a.respuestaQuejoso === 'ACEPTADO' ? 'ok' : 'gris' });
    }
    if (a?.fechaConclusion) {
      eventos.push({ fecha: a.fechaConclusion, titulo: 'Expediente concluido', detalle: 'Se registró el acuerdo de conclusión.', tipo: 'ok' });
    }

    return eventos
      .filter(ev => !!ev.fecha)
      .sort((x, y) => new Date(y.fecha).getTime() - new Date(x.fecha).getTime());
  }

  ngOnInit(): void {
    if (!this.folio) return;
    this.cargarExpediente();
  }

  /**
   * Abrir el detalle es "iniciar la investigación" (fila 14 del diagrama): un expediente
   * RECIBIDO pasa a EN_INVESTIGACION; en otro estatus solo se devuelve.
   */
  cargarExpediente(): void {
    this.expedienteService.iniciarInvestigacion(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = expediente;
        this.cargarHistorial();
        this.cargarPrimerContacto(expediente.folioOrigen);

        if (CON_ACUERDO.includes(expediente.estatus)) {
          this.cargarAcuerdo(expediente.id);
        } else {
          this.cdr.detectChanges();
        }
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar el expediente.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  cargarHistorial(): void {
    this.oficioService.historialPorFolio(this.folio).subscribe({
      next: (oficios) => {
        this.oficios = oficios;
        oficios.forEach(o => {
          this.recordatorioService.porOficio(o.id).subscribe({
            next: (lista) => {
              this.recordatorios[o.id] = lista;
              this.cdr.detectChanges();
            }
          });
          if (o.estatus === 'RESPONDIDO') {
            this.respuestaExternaService.porOficio(o.id).subscribe({
              next: (lista) => {
                this.respuestas[o.id] = lista[0];
                this.cdr.detectChanges();
              }
            });
          }
        });
        this.cdr.detectChanges();
      }
    });
  }

  cargarAcuerdo(expedienteId: number): void {
    this.acuerdoConclusionService.obtenerPorExpediente(expedienteId).subscribe({
      next: (acuerdo) => {
        this.acuerdo = acuerdo;
        this.textoAcuerdo = acuerdo?.textoAcuerdo ?? '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
      }
    });
  }

  pasoIndice(paso: string): number {
    return (this.pasos as string[]).indexOf(paso);
  }

  get pasoActualIndice(): number {
    return this.expediente ? this.pasoIndice(this.expediente.estatus) : -1;
  }

  guardarBorrador(): void {
    this.guardarOConcluir(false);
  }

  enviarAcuerdo(): void {
    if (!confirm('¿Enviar el acuerdo de conciliación al quejoso? El expediente quedará pendiente de su respuesta: si lo acepta, se elabora el acuerdo de conclusión; si lo rechaza, regresa a investigación.')) {
      return;
    }
    this.guardarOConcluir(true);
  }

  /** Fila 20: con el acuerdo de conciliación aceptado, el expediente se concluye. */
  registrarConclusion(): void {
    if (!this.expediente) return;
    if (!this.textoConclusion.trim()) {
      this.snackBar.open('Redacta el acuerdo de conclusión antes de continuar.', 'Cerrar', { duration: 3000 });
      return;
    }
    if (!confirm('¿Registrar el acuerdo de conclusión? El expediente se cerrará definitivamente.')) {
      return;
    }

    this.guardando = true;
    this.acuerdoConclusionService.registrarConclusion(this.expediente.id, this.textoConclusion.trim()).subscribe({
      next: () => {
        this.guardando = false;
        this.snackBar.open('Expediente concluido.', 'Cerrar', { duration: 3000 });
        this.cargarExpediente();
      },
      error: (err) => {
        this.guardando = false;
        this.snackBar.open(err?.error?.error ?? 'No fue posible registrar la conclusión.', 'Cerrar', { duration: 4000 });
      }
    });
  }

  /**
   * Igual que el botón de la bandeja: se puede oficiar mientras el expediente esté en
   * investigación y no haya un oficio esperando respuesta (el backend lo vuelve a validar).
   */
  get puedeSolicitarOficio(): boolean {
    return this.expediente?.estatus === 'EN_INVESTIGACION'
      && !this.oficios.some(o => o.estatus === 'EN_ESPERA' || o.estatus === 'VENCIDO');
  }

  solicitarOficio(): void {
    if (!this.expediente) return;

    const data: RedactarOficioDialogData = {
      expedienteId: this.expediente.id,
      folio: this.expediente.folio,
      siguienteFase: 'SOLICITUD_INFORMACION',
      unidadAcademica: this.expediente.unidadAcademica
    };

    this.dialog.open(RedactarOficioDialog, { width: '560px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Oficio generado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargarExpediente();
      }
    });
  }

  /** El quejoso rechazó el último acuerdo y el expediente volvió a investigación. */
  get acuerdoRechazado(): boolean {
    return this.expediente?.estatus === 'EN_INVESTIGACION' && this.acuerdo?.respuestaQuejoso === 'RECHAZADO';
  }

  // ------------------------------------------------------------------ PDF

  verPdfOficio(o: Oficio): void {
    this.abrirPdf(this.documentoService.oficio(o.id));
  }

  verPdfRecordatorio(r: Recordatorio): void {
    this.abrirPdf(this.documentoService.recordatorio(r.id));
  }

  verPdfAcuerdoConciliacion(): void {
    if (this.expediente) this.abrirPdf(this.documentoService.acuerdoConciliacion(this.expediente.id));
  }

  verPdfAcuerdoConclusion(): void {
    if (this.expediente) this.abrirPdf(this.documentoService.acuerdoConclusion(this.expediente.id));
  }

  verArchivoRespuesta(r: RespuestaExterna): void {
    this.abrirPdf(this.respuestaExternaService.archivo(r.id));
  }

  /** Adjuntar (o reemplazar) el archivo de una respuesta ya registrada. */
  adjuntarArchivoRespuesta(r: RespuestaExterna, evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) return;
    if (archivo.size > 10 * 1024 * 1024) {
      this.snackBar.open('El archivo excede el tamaño máximo de 10 MB.', 'Cerrar', { duration: 3500 });
      return;
    }
    this.respuestaExternaService.adjuntarArchivo(r.id, archivo).subscribe({
      next: (actualizada) => {
        this.respuestas[actualizada.oficioId] = actualizada;
        this.snackBar.open('Archivo adjuntado.', 'Cerrar', { duration: 3000 });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.error ?? 'No fue posible adjuntar el archivo.', 'Cerrar', { duration: 4000 });
      }
    });
  }

  private abrirPdf(peticion: Observable<Blob>): void {
    peticion.subscribe({
      next: (blob) => abrirBlob(blob),
      error: () => this.snackBar.open('No fue posible generar el PDF.', 'Cerrar', { duration: 3000 })
    });
  }

  esOficioActivo(o: Oficio): boolean {
    return o.estatus === 'EN_ESPERA' || o.estatus === 'VENCIDO';
  }

  abrirRecordatorio(o: Oficio): void {
    const data: RecordatorioDialogData = {
      oficioId: o.id,
      numeroOficio: o.numeroOficio,
      fase: o.fase
    };

    this.dialog.open(RecordatorioDialog, { width: '520px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Recordatorio registrado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargarExpediente();
      }
    });
  }

  abrirRespuestaExterna(o: Oficio): void {
    const data: RespuestaExternaDialogData = {
      oficioId: o.id,
      numeroOficio: o.numeroOficio,
      destinatarioNombre: o.destinatarioNombre
    };

    this.dialog.open(RespuestaExternaDialog, { width: '560px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Respuesta registrada correctamente.', 'Cerrar', { duration: 3000 });
        this.cargarExpediente();
      }
    });
  }

  private guardarOConcluir(concluir: boolean): void {
    if (!this.expediente) return;

    if (!this.textoAcuerdo.trim()) {
      this.snackBar.open('Redacta el texto del acuerdo antes de continuar.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.guardando = true;

    this.acuerdoConclusionService.guardarOConcluir({
      expedienteId: this.expediente.id,
      textoAcuerdo: this.textoAcuerdo.trim(),
      concluir
    }).subscribe({
      next: (acuerdo) => {
        this.guardando = false;
        this.acuerdo = acuerdo;
        this.snackBar.open(concluir ? 'Acuerdo enviado al quejoso. El expediente queda pendiente de su respuesta.' : 'Borrador guardado.', 'Cerrar', { duration: 3000 });
        this.cargarExpediente();
      },
      error: (err) => {
        this.guardando = false;
        const mensaje = err?.error?.error ?? 'No fue posible guardar el acuerdo.';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }
}
