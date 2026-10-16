import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { formatearFecha, formatearFechaHora } from '../../core/utils/estatus-expediente';
import { ResumenDialog } from '../resumen-dialog/resumen-dialog';

/**
 * Datos de una queja antecedente, ya sea un resultado de búsqueda o uno guardado. Se usa lo
 * que ya está cargado en pantalla: abrir el detalle no hace ninguna petición al servidor.
 */
export interface AntecedenteDetalleData {
  folioQueja: string;
  origen: string;
  folioPrimerContacto?: string;
  asunto?: string;
  fecha?: string;
  unidadAcademica?: string;
  nombreQuejoso?: string;
  nombreDenunciado?: string;
  estatus?: string;
  resultado?: string;
  descripcion?: string;
  extracto?: string;
  similitud?: number | null;
  coincidencias?: string[];
  /** Solo guardados. */
  fuente?: string;
  analistaNombre?: string;
  fechaRegistro?: string;
}

@Component({
  selector: 'app-antecedente-detalle-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './antecedente-detalle-dialog.html',
  styleUrl: './antecedente-detalle-dialog.css'
})
export class AntecedenteDetalleDialog {
  readonly formatearFecha = formatearFecha;
  readonly formatearFechaHora = formatearFechaHora;

  constructor(
    @Inject(MAT_DIALOG_DATA) public a: AntecedenteDetalleData,
    private dialogRef: MatDialogRef<AntecedenteDetalleDialog>,
    private dialog: MatDialog,
    private router: Router
  ) {}

  get narrativa(): string {
    return this.a.descripcion || this.a.extracto || '';
  }

  get origen(): string {
    return this.a.origen === 'HISTORICO' ? 'Caso histórico' : 'Queja del sistema';
  }

  get fuente(): string {
    switch (this.a.fuente) {
      case 'MANUAL': return 'Búsqueda manual';
      case 'MODELO': return 'Modelo';
      case 'REGLAS_PROVISIONAL': return 'Motor provisional';
      default: return this.a.fuente ?? '';
    }
  }

  verResumen(): void {
    this.dialog.open(ResumenDialog, {
      width: '640px',
      data: { folio: this.a.folioQueja, asunto: this.a.asunto, texto: this.narrativa }
    });
  }

  verExpediente(): void {
    this.dialogRef.close();
    this.router.navigate(['/expediente', this.a.folioPrimerContacto]);
  }
}
