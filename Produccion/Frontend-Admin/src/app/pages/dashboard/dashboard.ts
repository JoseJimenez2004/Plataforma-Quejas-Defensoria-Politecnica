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
