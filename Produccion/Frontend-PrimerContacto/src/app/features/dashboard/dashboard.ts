import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../../core/services/dashboard.service';
import { SesionPersonalService } from '../../core/services/sesion-personal.service';
import {
  DashboardCitaHoy,
  DashboardItemLista,
  DashboardResumen
} from '../../core/models/dashboard.model';
import { claseEstatus } from '../../core/utils/estatus-expediente';
import { Paginado, haceCuanto } from '../../core/utils/paginado';

/** Iconos (SVG en línea, mismo trazo que Recepción) de cada indicador. */
const ICONOS: Record<DashboardResumen['tipo'], string> = {
  PENDIENTES: 'M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 12h6M9 16h4',
  CON_CITA: 'M5 4.5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-12a2 2 0 0 1 2-2zM3 9.5h18M8 2.5v4M16 2.5v4',
  EN_DICTAMEN: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 13h8M8 17h5',
  REMITIDOS: 'M22 12h-6M19 9l3 3-3 3M20 6V5a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-1M2 5l9 6 4-2.5'
};

const DETALLES: Record<DashboardResumen['tipo'], string> = {
  PENDIENTES: 'por atender',
  CON_CITA: 'con cita programada',
  EN_DICTAMEN: 'procedentes e improcedentes',
  REMITIDOS: 'a otra instancia'
};

/** Panel de inicio (rediseño 2026-11, homologado con Recepción). */
@Component({
  selector: 'app-dashboard',
  imports: [FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['../../shared/estilos/recepcion.css', './dashboard.css']
})
export class Dashboard implements OnInit {
  readonly fechaActual = this.obtenerFechaActual();
  readonly claseEstatus = claseEstatus;
  readonly haceCuanto = haceCuanto;
  readonly iconos = ICONOS;
  readonly detalles = DETALLES;

  resumen: DashboardResumen[] = [];
  citasHoy: DashboardCitaHoy[] = [];
  lista: DashboardItemLista[] = [];

  /** PC-4: estados de carga de Inicio. */
  cargando = true;
  error = false;
  errorCitas = false;

  categoriaSeleccionada: DashboardResumen['tipo'] = 'PENDIENTES';
  readonly paginado = new Paginado<DashboardItemLista>(10);
  readonly tamaniosPagina = [5, 10, 20];

  constructor(
    private router: Router,
    private dashboardService: DashboardService,
    private sesion: SesionPersonalService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  /** Nombre de pila para el saludo ("Analista Demo Primer Contacto" -> "Analista Demo"). */
  get saludo(): string {
    const partes = (this.sesion.getNombre() ?? '').split(/\s+/).filter(Boolean);
    return partes.slice(0, 2).join(' ');
  }

  cargar(): void {
    this.cargando = true;
    this.error = false;
    this.errorCitas = false;
    this.cdr.detectChanges();

    this.dashboardService.obtenerPanel().subscribe({
      next: (panel) => {
        this.resumen = panel.resumen;
        this.lista = panel.lista;
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

  seleccionarCategoria(tipo: DashboardResumen['tipo']): void {
    this.categoriaSeleccionada = tipo;
    this.paginado.pagina = 0;
  }

  get listaSeleccionada(): DashboardItemLista[] {
    return this.lista.filter(item => item.tipo === this.categoriaSeleccionada);
  }

  get filasPagina(): DashboardItemLista[] {
    return this.paginado.cortar(this.listaSeleccionada);
  }

  irAPagina(p: number): void {
    this.paginado.irA(p, this.listaSeleccionada.length);
  }

  get tituloLista(): string {
    switch (this.categoriaSeleccionada) {
      case 'PENDIENTES': return 'Expedientes pendientes';
      case 'CON_CITA': return 'Expedientes con cita';
      case 'EN_DICTAMEN': return 'Expedientes dictaminados';
      case 'REMITIDOS': return 'Expedientes remitidos';
      default: return 'Expedientes';
    }
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

  private obtenerFechaActual(): string {
    const fecha = new Date().toLocaleDateString('es-MX', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
    return fecha.charAt(0).toUpperCase() + fecha.slice(1);
  }
}
