import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { Queja, etiquetaEstatus } from '../../../core/models/queja.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-mis-quejas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mis-quejas.html',
  styleUrl: './mis-quejas.scss',
})
export class MisQuejas implements OnInit {
  filtroFolio = '';
  filtroEstatus = 'Todos';

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

  get quejasFiltradas(): Queja[] {
    return this.quejas.filter((q) => {
      const coincideFolio = !this.filtroFolio || q.numeroFolio.includes(this.filtroFolio);
      const coincideEstatus =
        this.filtroEstatus === 'Todos' || this.etiqueta(q.estatus) === this.filtroEstatus;
      return coincideFolio && coincideEstatus;
    });
  }
}
