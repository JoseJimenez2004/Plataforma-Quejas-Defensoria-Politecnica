import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { ConciliacionService } from '../../../core/services/conciliacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { Queja, etiquetaEstatus } from '../../../core/models/queja.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-resumen',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './resumen.html',
  styleUrl: './resumen.scss',
})
export class Resumen implements OnInit {
  quejas: Queja[] = [];
  acuerdosPendientes = 0;
  cargando = true;
  error = '';

  constructor(
    public authService: AuthService,
    private quejaService: QuejaService,
    private conciliacionService: ConciliacionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.quejaService.misQuejas().subscribe({
      next: (quejas) => {
        this.quejas = quejas;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudieron cargar tus quejas.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });

    this.conciliacionService.misAcuerdos().subscribe({
      next: (acuerdos) => {
        this.acuerdosPendientes = acuerdos.filter((a) => a.estado === 'PENDIENTE').length;
        this.cdr.detectChanges();
      },
      error: () => {
        // No es crítico para el resumen si conciliación falla en cargar.
      },
    });
  }

  etiqueta(estatus: string | null | undefined): string {
    return etiquetaEstatus(estatus);
  }

  get totales(): number {
    return this.quejas.length;
  }

  get enProceso(): number {
    return this.quejas.filter((q) => etiquetaEstatus(q.estatus) !== 'Finalizada').length;
  }

  get finalizadas(): number {
    return this.quejas.filter((q) => etiquetaEstatus(q.estatus) === 'Finalizada').length;
  }

  /** Solo las 5 más recientes — el detalle completo con filtros vive en "Mis Quejas", el
   * resumen es una vista ejecutiva rápida, no una segunda copia de la tabla completa. */
  get quejasRecientes(): Queja[] {
    return [...this.quejas]
      .sort((a, b) => (b.fechaCreacion ?? '').localeCompare(a.fechaCreacion ?? ''))
      .slice(0, 5);
  }

  get primerNombre(): string {
    const nombre = this.authService.usuarioActual()?.nombre ?? '';
    return nombre.split(' ')[0] || nombre;
  }
}
