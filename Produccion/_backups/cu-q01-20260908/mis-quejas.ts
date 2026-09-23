import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { Queja, etiquetaEstatus } from '../../../core/models/queja.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { ToastService } from '../../../core/services/toast.service';
import { Datepicker } from '../../../shared/datepicker/datepicker';

@Component({
  selector: 'app-mis-quejas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Datepicker],
  templateUrl: './mis-quejas.html',
  styleUrl: './mis-quejas.scss',
})
export class MisQuejas implements OnInit {
  filtroFolio = '';
  filtroAsunto = '';
  filtroUnidadAcademica = '';
  filtroFecha = '';
  filtroEstatus = 'Todos';

  quejas: Queja[] = [];
  dependencias: Dependencia[] = [];
  cargando = true;
  error = '';

  constructor(
    private quejaService: QuejaService,
    private catalogoService: CatalogoService,
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

    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cdr.detectChanges();
      },
      error: () => {
        // No es crítico: el filtro de unidad académica simplemente no tendrá opciones.
      },
    });
  }

  etiqueta(estatus: string | null | undefined): string {
    return etiquetaEstatus(estatus);
  }

  nombreUnidad(clave?: string): string {
    if (!clave) return '—';
    return this.dependencias.find((d) => d.clave === clave)?.nombre ?? clave;
  }

  limpiarFiltros(): void {
    this.filtroFolio = '';
    this.filtroAsunto = '';
    this.filtroUnidadAcademica = '';
    this.filtroFecha = '';
    this.filtroEstatus = 'Todos';
  }

  get hayFiltrosActivos(): boolean {
    return !!(
      this.filtroFolio ||
      this.filtroAsunto ||
      this.filtroUnidadAcademica ||
      this.filtroFecha ||
      this.filtroEstatus !== 'Todos'
    );
  }

  get quejasFiltradas(): Queja[] {
    return this.quejas.filter((q) => {
      const coincideFolio = !this.filtroFolio || q.numeroFolio.includes(this.filtroFolio);
      const coincideAsunto =
        !this.filtroAsunto || (q.motivo ?? '').toLowerCase().includes(this.filtroAsunto.toLowerCase());
      const coincideUnidad =
        !this.filtroUnidadAcademica || q.unidadAcademicaClave === this.filtroUnidadAcademica;
      const coincideFecha = !this.filtroFecha || (q.fechaCreacion ?? '').slice(0, 10) === this.filtroFecha;
      const coincideEstatus =
        this.filtroEstatus === 'Todos' || this.etiqueta(q.estatus) === this.filtroEstatus;
      return coincideFolio && coincideAsunto && coincideUnidad && coincideFecha && coincideEstatus;
    });
  }
}
