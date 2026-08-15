import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ConciliacionService } from '../../../core/services/conciliacion.service';
import { AcuerdoConciliacion } from '../../../core/models/conciliacion.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-conciliacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './conciliacion.html',
  styleUrl: './conciliacion.scss',
})
export class Conciliacion implements OnInit {
  acuerdos: AcuerdoConciliacion[] = [];
  cargando = true;
  error = '';

  acuerdoSeleccionado: AcuerdoConciliacion | null = null;
  comentario = '';
  enviando = false;

  constructor(
    private conciliacionService: ConciliacionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.conciliacionService.misAcuerdos().subscribe({
      next: (acuerdos) => {
        this.acuerdos = acuerdos;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudieron cargar tus acuerdos de conciliación.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });
  }

  get pendientes(): AcuerdoConciliacion[] {
    return this.acuerdos.filter((a) => a.estado === 'PENDIENTE');
  }

  get resueltos(): AcuerdoConciliacion[] {
    return this.acuerdos.filter((a) => a.estado !== 'PENDIENTE');
  }

  verPropuesta(acuerdo: AcuerdoConciliacion): void {
    this.acuerdoSeleccionado = acuerdo;
    this.comentario = '';
  }

  cerrarPropuesta(): void {
    this.acuerdoSeleccionado = null;
  }

  responder(estado: 'ACEPTADO' | 'RECHAZADO'): void {
    if (!this.acuerdoSeleccionado) return;
    this.enviando = true;
    this.conciliacionService
      .responder(this.acuerdoSeleccionado.id, { estado, comentario: this.comentario || undefined })
      .subscribe({
        next: (actualizado) => {
          this.enviando = false;
          const idx = this.acuerdos.findIndex((a) => a.id === actualizado.id);
          if (idx !== -1) this.acuerdos[idx] = actualizado;
          this.toast.exito(
            estado === 'ACEPTADO' ? 'Aceptaste el acuerdo de conciliación.' : 'Rechazaste el acuerdo de conciliación.',
          );
          this.acuerdoSeleccionado = null;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.enviando = false;
          this.toast.error(err?.error?.mensaje ?? 'No se pudo registrar tu respuesta. Intenta de nuevo.');
          this.cdr.detectChanges();
        },
      });
  }
}
