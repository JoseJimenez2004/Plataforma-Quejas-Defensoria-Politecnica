import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RespuestaExternaService } from '../../core/services/respuesta-externa.service';
import { RespuestaExterna } from '../../core/models/respuesta-externa';

export interface RespuestaExternaDialogData {
  oficioId: number;
  numeroOficio: string;
  destinatarioNombre: string;
}

@Component({
  selector: 'app-respuesta-externa-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './respuesta-externa-dialog.html',
  styleUrl: './respuesta-externa-dialog.css'
})
export class RespuestaExternaDialog {
  canalRecepcion = '';
  numeroOficioRespuestaUA = '';
  resumen = '';
  enviando = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RespuestaExternaDialogData,
    private dialogRef: MatDialogRef<RespuestaExternaDialog>,
    private respuestaExternaService: RespuestaExternaService,
    private snackBar: MatSnackBar
  ) {}

  cancelar(): void {
    this.dialogRef.close();
  }

  confirmar(): void {
    if (!this.canalRecepcion.trim() || !this.resumen.trim()) {
      this.snackBar.open('Indica el canal de recepción y el resumen de la información recibida.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.enviando = true;

    this.respuestaExternaService.registrarRespuesta({
      oficioId: this.data.oficioId,
      canalRecepcion: this.canalRecepcion.trim(),
      numeroOficioRespuestaUA: this.numeroOficioRespuestaUA.trim() || undefined,
      resumen: this.resumen.trim()
    }).subscribe({
      next: (respuesta: RespuestaExterna) => {
        this.dialogRef.close(respuesta);
      },
      error: (err) => {
        this.enviando = false;
        const mensaje = err?.error?.error ?? 'No fue posible registrar la respuesta.';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }
}
