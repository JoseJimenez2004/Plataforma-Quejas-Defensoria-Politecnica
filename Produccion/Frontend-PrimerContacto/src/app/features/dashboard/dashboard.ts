import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { DashboardService } from '../../core/services/dashboard.service';
import {
  DashboardActividad,
  DashboardCitaHoy,
  DashboardItemLista,
  DashboardResumen
} from '../../core/models/dashboard.model';
import { clasePrioridad } from '../../core/utils/estatus-expediente';

@Component({
  selector: 'app-dashboard',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
fechaActual = this.obtenerFechaActual();
readonly clasePrioridad = clasePrioridad;
resumen: DashboardResumen[] = [];

actividad: DashboardActividad[] = [];
citasHoy: DashboardCitaHoy[] = [];
lista: DashboardItemLista[] = [];


constructor(
  private router: Router,
  private dashboardService: DashboardService,
  private cdr: ChangeDetectorRef
) {}
  navegar(ruta: string): void {
    this.router.navigate([ruta]);
  }

  abrirExpediente(folio: string): void {
    this.router.navigate(['/expediente', folio]);
  }

  irABandeja(): void {
    this.router.navigate(['/bandeja']);
  }

  irAgenda(): void {
    this.router.navigate(['/agenda']);
  }
  /** PC-4: estados de carga de Inicio. */
  cargando = true;
  error = false;
  errorCitas = false;

  ngOnInit(): void {
    this.cargar();
  }

  /** Una sola petición a la bandeja (indicadores, lista y actividad) y otra a la agenda. */
  cargar(): void {
    this.cargando = true;
    this.error = false;
    this.errorCitas = false;
    this.cdr.detectChanges();

    this.dashboardService.obtenerPanel().subscribe({
      next: (panel) => {
        this.resumen = panel.resumen;
        this.lista = panel.lista;
        this.actividad = panel.actividad;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });

    this.dashboardService.obtenerCitasHoy().subscribe({
      next: (data) => {
        this.citasHoy = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorCitas = true;
        this.cdr.detectChanges();
      }
    });
  }
categoriaSeleccionada: DashboardResumen['tipo'] = 'PENDIENTES';

seleccionarCategoria(tipo: DashboardResumen['tipo']): void {
  this.categoriaSeleccionada = tipo;
}
obtenerListaSeleccionada(): DashboardItemLista[] {
  return this.lista.filter(item => item.tipo === this.categoriaSeleccionada);
}
get tituloLista(): string {
  switch (this.categoriaSeleccionada) {
    case 'PENDIENTES':
      return 'Expedientes pendientes';

    case 'CON_CITA':
      return 'Expedientes con cita';

    case 'EN_DICTAMEN':
      return 'Expedientes dictaminados (procedentes e improcedentes)';

    case 'REMITIDOS':
      return 'Expedientes remitidos';

    default:
      return 'Expedientes';
  }
}

private obtenerFechaActual(): string {
  const fecha = new Date().toLocaleDateString('es-MX', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return fecha.charAt(0).toUpperCase() + fecha.slice(1);
}

}