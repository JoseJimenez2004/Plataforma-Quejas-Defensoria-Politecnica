import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ResumenSimulado, generarResumenSimulado } from '../../core/utils/resumen-simulado';

export interface ResumenDialogData {
  /** Folio de la queja (FOL-... o el del caso histórico). */
  folio: string;
  asunto?: string;
  /** Narrativa completa de la queja. */
  texto?: string;
}

/**
 * Resumen propuesto y frases de impacto de una queja. Hoy es SIMULADO (se calcula en el
 * navegador, ver core/utils/resumen-simulado.ts) y la pantalla lo dice claramente.
 */
@Component({
  selector: 'app-resumen-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './resumen-dialog.html',
  styleUrl: './resumen-dialog.css'
})
export class ResumenDialog {
  readonly resultado: ResumenSimulado;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ResumenDialogData) {
    this.resultado = generarResumenSimulado(data.texto);
  }
}
