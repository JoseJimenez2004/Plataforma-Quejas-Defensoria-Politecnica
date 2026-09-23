import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RecordatorioService } from '../../core/services/recordatorio.service';
import { Recordatorio } from '../../core/models/recordatorio';

export interface RecordatorioDialogData {
  oficioId: number;
  numeroOficio: string;
  fase: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR';
}

@Component({
  selector: 'app-recordatorio-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './recordatorio-dialog.html',
  styleUrl: './recordatorio-dialog.css'
})
export class RecordatorioDialog {
  mensaje = '';
  medidasOfrecidas = '';
  enviando = false;

  get mostrarMedidas(): boolean {
    return this.data.fase === 'GESTION_DIRECTOR';
  }

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RecordatorioDialogData,
    private dialogRef: MatDialogRef<RecordatorioDialog>,
    private recordatorioService: RecordatorioService,
    private snackBar: MatSnackBar
  ) {}

  cancelar(): void {
    this.dialogRef.close();
  }

  enviar(): void {
    if (!this.mensaje.trim()) {
      this.snackBar.open('Escribe el mensaje del recordatorio.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.enviando = true;

    this.recordatorioService.generarRecordatorio({
      oficioId: this.data.oficioId,
      mensaje: this.mensaje.trim(),
      medidasOfrecidas: this.medidasOfrecidas.trim() || undefined
    }).subscribe({
      next: (recordatorio: Recordatorio) => {
        this.dialogRef.close(recordatorio);
      },
      error: (err) => {
        this.enviando = false;
        const mensaje = err?.error?.error ?? 'No fue posible generar el recordatorio.';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }
}
