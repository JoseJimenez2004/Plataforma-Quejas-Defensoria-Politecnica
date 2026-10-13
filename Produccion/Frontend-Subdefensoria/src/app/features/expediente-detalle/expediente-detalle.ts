import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
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
import { Observable } from 'rxjs';
import { ExpedienteInvestigacion } from '../../core/models/expediente-investigacion';
import { Oficio } from '../../core/models/oficio';
import { AcuerdoConclusion } from '../../core/models/acuerdo-conclusion';
import { ETIQUETAS_ESTATUS, PASOS_EXPEDIENTE, etiquetaEstatus } from '../../core/models/estatus-expediente';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';
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
  acuerdo: AcuerdoConclusion | null = null;
  textoAcuerdo = '';
  textoConclusion = '';
  guardando = false;

  readonly pasos = PASOS_EXPEDIENTE;
  readonly etiquetas: Record<string, string> = ETIQUETAS_ESTATUS;
  readonly iconos = ICONOS_PASO;
  readonly etiquetaEstatus = etiquetaEstatus;

  constructor(
    private route: ActivatedRoute,
    private expedienteService: ExpedienteService,
    private oficioService: OficioService,
    private acuerdoConclusionService: AcuerdoConclusionService,
    private recordatorioService: RecordatorioService,
    private documentoService: DocumentoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
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
        oficios.forEach(o => this.recordatorioService.porOficio(o.id).subscribe({
          next: (lista) => {
            this.recordatorios[o.id] = lista;
            this.cdr.detectChanges();
          }
        }));
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
