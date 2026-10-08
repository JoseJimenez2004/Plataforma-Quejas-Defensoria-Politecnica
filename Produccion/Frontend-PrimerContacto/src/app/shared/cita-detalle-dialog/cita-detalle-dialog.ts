import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CitaPrimerContacto } from '../../core/models/cita-primer-contacto';
import { formatearFechaHora } from '../../core/utils/estatus-expediente';

@Component({
  selector: 'app-cita-detalle-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './cita-detalle-dialog.html',
  styleUrl: './cita-detalle-dialog.css'
})
export class CitaDetalleDialog {
  /** Formulario para registrar que el quejoso canceló, con su motivo. */
  registrandoCancelacion = false;
  motivoCancelacion = '';

  readonly formatearFechaHora = formatearFechaHora;

  constructor(
    @Inject(MAT_DIALOG_DATA) public cita: CitaPrimerContacto,
    private dialogRef: MatDialogRef<CitaDetalleDialog>,
    private router: Router
  ) {}

  get cancelada(): boolean {
    return this.cita.estatusCodigo === 'CANCELADA';
  }

  get confirmada(): boolean {
    return this.cita.estatusCodigo === 'CONFIRMADA';
  }

  /** Esperando la respuesta del quejoso, o su plazo ya venció sin respuesta. */
  get pendienteDeRespuesta(): boolean {
    return this.cita.estatusCodigo === 'PROGRAMADA' || this.cita.estatusCodigo === 'SIN_RESPUESTA';
  }

  get canceladaPorQuejoso(): boolean {
    return this.cita.estatusCodigo === 'CANCELADA_QUEJOSO';
  }

  get quienRespondio(): string {
    return this.cita.respuestaRegistradaPor === 'QUEJOSO'
      ? 'el quejoso, desde su panel'
      : 'Primer Contacto, a nombre del quejoso';
  }

  cerrar(): void {
    this.dialogRef.close();
  }

  verExpediente(): void {
    this.dialogRef.close();
    this.router.navigate(['/expediente', this.cita.folio]);
  }

  confirmar(): void {
    this.dialogRef.close({ accion: 'confirmar', cita: this.cita });
  }

  reagendar(): void {
    this.dialogRef.close({ accion: 'reagendar', cita: this.cita });
  }

  cancelar(): void {
    this.dialogRef.close({ accion: 'cancelar', cita: this.cita });
  }

  registrarCancelacionQuejoso(): void {
    if (!this.motivoCancelacion.trim()) return;
    this.dialogRef.close({
      accion: 'cancelacion-quejoso',
      cita: this.cita,
      motivo: this.motivoCancelacion.trim()
    });
  }
}
