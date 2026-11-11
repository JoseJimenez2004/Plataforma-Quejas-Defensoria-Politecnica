import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

/** Datos del aviso de confirmación (mismo diseño que el portal del quejoso). */
export interface ConfirmarDialogData {
  titulo: string;
  /** Texto principal, debajo del título. */
  mensaje?: string;
  /** Recuadro gris con lo que pasará después (opcional). */
  nota?: string;
  textoConfirmar?: string;
  textoCancelar?: string;
  /** 'peligro' = rojo (cancelar, eliminar, improcedente); 'normal' = guinda. */
  tono?: 'peligro' | 'normal';
  /** Nombre del ícono de Material Icons. */
  icono?: string;
}

@Component({
  selector: 'app-confirmar-dialog',
  imports: [MatDialogModule, MatIconModule],
  templateUrl: './confirmar-dialog.html',
  styleUrl: './confirmar-dialog.css'
})
export class ConfirmarDialog {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ConfirmarDialogData,
    private ref: MatDialogRef<ConfirmarDialog, boolean>
  ) {}

  get peligro(): boolean {
    return this.data.tono === 'peligro';
  }

  get icono(): string {
    return this.data.icono || (this.peligro ? 'warning_amber' : 'help_outline');
  }

  responder(valor: boolean): void {
    this.ref.close(valor);
  }
}
