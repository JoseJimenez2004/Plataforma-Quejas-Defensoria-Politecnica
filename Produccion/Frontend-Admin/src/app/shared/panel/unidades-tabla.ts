import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { FilaUnidad, FiltroUnidades, TablaUnidades } from '../../core/models/admin.models';
import { Paginador } from './paginador';
import { descargarCsv, fechaCorta } from './csv';

export const TIPOS_UNIDAD: { clave: string; etiqueta: string }[] = [
  { clave: 'ESCUELA_SUPERIOR', etiqueta: 'Escuela Superior' },
  { clave: 'UNIDAD_PROFESIONAL', etiqueta: 'Unidad Profesional' },
  { clave: 'MEDIO_SUPERIOR', etiqueta: 'Nivel Medio Superior' },
  { clave: 'INVESTIGACION', etiqueta: 'Centro de Investigación' },
  { clave: 'OTROS', etiqueta: 'Administración central / otra' },
];

/**
 * Tabla de dependencias y unidades con quejas. {@code completa} agrega nombre oficial, siglas
 * y tipo (pantalla "Quejas por unidad"); sin ella es la versión corta de la ventana del panel.
 */
@Component({
  selector: 'app-unidades-tabla',
  standalone: true,
  imports: [FormsModule, Paginador],
  templateUrl: './unidades-tabla.html',
})
export class UnidadesTabla implements OnChanges, OnDestroy {
  @Input() dias = 365;
  @Input() completa = false;
  @Input() tamanio = 10;

  datos: TablaUnidades | null = null;
  cargando = true;
  pagina = 0;
  filtro: FiltroUnidades = {};
  readonly tipos = TIPOS_UNIDAD;
  readonly fechaCorta = fechaCorta;
  private espera: ReturnType<typeof setTimeout> | null = null;

  constructor(private seguimiento: SeguimientoService, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnChanges(): void {
    this.pagina = 0;
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.espera) {
      clearTimeout(this.espera);
    }
  }

  buscar(): void {
    if (this.espera) {
      clearTimeout(this.espera);
    }
    this.espera = setTimeout(() => this.aplicar(), 300);
  }

  aplicar(): void {
    this.pagina = 0;
    this.cargar();
  }

  limpiar(): void {
    this.filtro = {};
    this.aplicar();
  }

  irA(p: number): void {
    this.pagina = p;
    this.cargar();
  }

  ver(u: FilaUnidad): void {
    this.router.navigate(['/quejas'], { queryParams: { unidad: u.clave } });
  }

  /** Exporta todas las unidades que cumplen los filtros (no solo la página). */
  exportar(): void {
    this.seguimiento.tablaUnidades(this.filtro, this.dias, 0, 500).subscribe((t) => {
      descargarCsv('quejas-por-unidad', ['#', 'Unidad / dependencia', 'Siglas', 'Tipo', 'Quejas', 'En trámite', 'Cerradas', '% del total', 'Última recepción'],
        t.filas.map((u) => [u.posicion, u.nombre || u.clave, u.siglas, u.tipoEtiqueta, u.total, u.enTramite, u.cerradas, u.porcentaje + '%', fechaCorta(u.ultima)]));
    });
  }

  private cargar(): void {
    this.cargando = true;
    const f = { ...this.filtro, dias: undefined } as FiltroUnidades;
    this.seguimiento.tablaUnidades(f, this.filtro.desde || this.filtro.hasta ? 0 : this.dias, this.pagina, this.tamanio).subscribe({
      next: (d) => { this.datos = d; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.cdr.detectChanges(); },
    });
  }
}
