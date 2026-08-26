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
import { ExpedienteInvestigacion } from '../../core/models/expediente-investigacion';
import { Oficio } from '../../core/models/oficio';
import { AcuerdoConclusion } from '../../core/models/acuerdo-conclusion';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';
import { RespuestaExternaDialog, RespuestaExternaDialogData } from '../../shared/respuesta-externa-dialog/respuesta-externa-dialog';

const PASOS = ['RECIBIDO', 'EN_INVESTIGACION', 'EN_GESTION_DIRECTOR', 'LISTO_A_DICTAMINAR', 'CONCLUIDO'];

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
  acuerdo: AcuerdoConclusion | null = null;
  textoAcuerdo = '';
  guardando = false;

  readonly pasos = PASOS;

  constructor(
    private route: ActivatedRoute,
    private expedienteService: ExpedienteService,
    private oficioService: OficioService,
    private acuerdoConclusionService: AcuerdoConclusionService,
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

  cargarExpediente(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = expediente;
        this.cargarHistorial();

        if (expediente.estatus === 'LISTO_A_DICTAMINAR' || expediente.estatus === 'CONCLUIDO') {
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
    return this.pasos.indexOf(paso);
  }

  get pasoActualIndice(): number {
    return this.expediente ? this.pasoIndice(this.expediente.estatus) : -1;
  }

  guardarBorrador(): void {
    this.guardarOConcluir(false);
  }

  concluirExpediente(): void {
    if (!confirm('¿Confirmas que deseas concluir este expediente? Esta acción cierra la investigación y lo envía al área secretarial.')) {
      return;
    }
    this.guardarOConcluir(true);
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
        this.snackBar.open(concluir ? 'Expediente concluido correctamente.' : 'Borrador guardado.', 'Cerrar', { duration: 3000 });
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
