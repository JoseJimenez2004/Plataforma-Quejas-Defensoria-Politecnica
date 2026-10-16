import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

export interface SolicitudDenunciadoDialogData {
  folioQueja: string;
  denunciado?: string;
  /** Correo de la última solicitud, para reenviar sin volver a escribirlo. */
  correoAnterior?: string;
}

const FORMATO_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** Pide el correo del denunciado; el envío lo hace la pantalla del expediente. */
@Component({
  selector: 'app-solicitud-denunciado-dialog',
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './solicitud-denunciado-dialog.html',
  styleUrl: './solicitud-denunciado-dialog.css'
})
export class SolicitudDenunciadoDialog {
  correo: string;
  intentoEnviar = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: SolicitudDenunciadoDialogData,
    private dialogRef: MatDialogRef<SolicitudDenunciadoDialog, string>
  ) {
    this.correo = data.correoAnterior ?? '';
  }

  get correoValido(): boolean {
    return FORMATO_CORREO.test(this.correo.trim());
  }

  enviar(): void {
    this.intentoEnviar = true;
    if (!this.correoValido) return;
    this.dialogRef.close(this.correo.trim());
  }
}
