import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardResumen } from '../../core/models/admin.models';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  resumen: DashboardResumen | null = null;
  cargando = true;

  constructor(
    private dashboardService: DashboardService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  private static readonly MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

  maximo(lista: { total: number }[]): number {
    return lista.reduce((max, x) => Math.max(max, x.total), 0);
  }

  /** "2026-10" -> "Oct" */
  nombreMes(clave: string): string {
    const mes = Number(clave.split('-')[1]);
    return Dashboard.MESES[mes - 1] ?? clave;
  }

  ngOnInit(): void {
    this.dashboardService.resumen().subscribe({
      next: (resumen) => {
        this.resumen = resumen;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el resumen del sistema.');
        this.cdr.detectChanges();
      },
    });
  }
}
