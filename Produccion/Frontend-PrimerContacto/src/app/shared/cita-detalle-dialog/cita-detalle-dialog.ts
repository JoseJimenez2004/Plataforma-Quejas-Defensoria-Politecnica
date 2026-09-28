import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { CitaPrimerContacto } from '../../core/models/cita-primer-contacto';

@Component({
  selector: 'app-cita-detalle-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule],
  templateUrl: './cita-detalle-dialog.html',
  styleUrl: './cita-detalle-dialog.css'
})
export class CitaDetalleDialog {
  constructor(
    @Inject(MAT_DIALOG_DATA) public cita: CitaPrimerContacto,
    private dialogRef: MatDialogRef<CitaDetalleDialog>,
    private router: Router
  ) {}

  get cancelada(): boolean {
    return this.cita.estatus === 'Cancelada';
  }

  get confirmada(): boolean {
    return this.cita.estatus === 'Confirmada';
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
}
