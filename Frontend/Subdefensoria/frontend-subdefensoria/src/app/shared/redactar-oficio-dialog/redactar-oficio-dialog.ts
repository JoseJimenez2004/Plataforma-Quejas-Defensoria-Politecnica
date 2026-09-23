import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OficioService } from '../../core/services/oficio.service';
import { Oficio } from '../../core/models/oficio';

export interface RedactarOficioDialogData {
  expedienteId: number;
  folio: string;
  siguienteFase: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR';
  unidadAcademica: string;
}

@Component({
  selector: 'app-redactar-oficio-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './redactar-oficio-dialog.html',
  styleUrl: './redactar-oficio-dialog.css'
})
export class RedactarOficioDialog {
  destinatarioNombre = '';
  destinatarioCorreo = '';
  unidadAcademica: string;
  contenidoRedactado = '';
  enviando = false;

  get tituloAccion(): string {
    return this.data.siguienteFase === 'GESTION_DIRECTOR'
      ? 'Oficio al Director'
      : 'Solicitud de Información (Primer Oficio)';
  }

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RedactarOficioDialogData,
    private dialogRef: MatDialogRef<RedactarOficioDialog>,
    private oficioService: OficioService,
    private snackBar: MatSnackBar
  ) {
    this.unidadAcademica = data.unidadAcademica ?? '';
  }

  cancelar(): void {
    this.dialogRef.close();
  }

  enviar(): void {
    if (!this.destinatarioNombre.trim() || !this.destinatarioCorreo.trim() || !this.contenidoRedactado.trim()) {
      this.snackBar.open('Completa destinatario, correo y contenido del oficio.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.enviando = true;

    this.oficioService.crearOficio({
      expedienteId: this.data.expedienteId,
      destinatarioNombre: this.destinatarioNombre.trim(),
      destinatarioCorreo: this.destinatarioCorreo.trim(),
      unidadAcademica: this.unidadAcademica.trim(),
      contenidoRedactado: this.contenidoRedactado.trim()
    }).subscribe({
      next: (oficio: Oficio) => {
        this.dialogRef.close(oficio);
      },
      error: (err) => {
        this.enviando = false;
        const mensaje = err?.error?.error ?? 'No fue posible generar el oficio.';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }
}
