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
  fechaActual = 'Lunes 29 de junio de 2026';

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
  ngOnInit(): void {

  this.dashboardService.obtenerResumen()
    .subscribe(data => {
      this.resumen = data;
      this.cdr.detectChanges();
    });

  this.dashboardService.obtenerActividad()
    .subscribe(data => {
      this.actividad = data;
      this.cdr.detectChanges();
    });

  this.dashboardService.obtenerCitasHoy()
    .subscribe(data => {
      this.citasHoy = data;
      this.cdr.detectChanges();
    });

  this.dashboardService.obtenerLista()
    .subscribe(data => {
      this.lista = data;
      this.cdr.detectChanges();
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
      return 'Expedientes en elaboración de dictamen';

    case 'REMITIDOS':
      return 'Expedientes remitidos';

    default:
      return 'Expedientes';
  }
}
}