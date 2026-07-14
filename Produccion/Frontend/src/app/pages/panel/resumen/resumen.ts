import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
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
  cargando = true;
  error = '';

  constructor(
    private quejaService: QuejaService,
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
}
