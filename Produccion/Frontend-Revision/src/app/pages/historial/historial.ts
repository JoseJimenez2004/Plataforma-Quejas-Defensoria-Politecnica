import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { HistorialService } from '../../core/services/historial.service';
import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { HistorialFila, HistorialPagina, QuejaDetalle } from '../../core/models/revision.models';

const TAMANIO_PAGINA = 10;

type FiltroResultado = '' | 'TURNADO' | 'RECHAZADO';

/**
 * Historial de Recepción (2026-10-16) con el mismo formato que la Bandeja: totales del
 * periodo, búsqueda, filtros por resultado y por quién atendió, y lista paginada (10 por
 * página) con la queja atendida más recientemente arriba.
 */
@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historial.html',
  styleUrl: './historial.scss',
})
export class Historial implements OnInit, OnDestroy {
  readonly opcionesResultado: { valor: FiltroResultado; etiqueta: string }[] = [
    { valor: '', etiqueta: 'Todas' },
    { valor: 'TURNADO', etiqueta: 'Turnadas' },
    { valor: 'RECHAZADO', etiqueta: 'Con observaciones' },
  ];

  datos: HistorialPagina | null = null;
  cargando = true;
  exportando = false;

  texto = '';
  resultado: FiltroResultado = '';
  soloMias = false;
  fechaDesde = '';
  fechaHasta = '';
  pagina = 0;

  mostrarModalDetalle = false;
  detalleSeleccionado: QuejaDetalle | null = null;

  readonly fechaHoy = (() => {
    const t = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date());
    return t.charAt(0).toUpperCase() + t.slice(1);
  })();

  private busquedaId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private historialService: HistorialService,
    private revisionService: RevisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
  }

  // ---------------- filtros ----------------

  cambiarResultado(valor: FiltroResultado): void {
    this.resultado = valor;
    this.recargarDesdeInicio();
  }

  cambiarSoloMias(valor: boolean): void {
    this.soloMias = valor;
    this.recargarDesdeInicio();
  }

  buscar(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
    this.busquedaId = setTimeout(() => this.recargarDesdeInicio(), 350);
  }

  cambiarFechas(): void {
    if (this.fechaDesde && this.fechaHasta && this.fechaDesde > this.fechaHasta) {
      this.toast.advertencia('La fecha "Desde" no puede ser posterior a la fecha "Hasta".');
      return;
    }
    this.recargarDesdeInicio();
  }

  limpiarFiltros(): void {
    this.texto = '';
    this.resultado = '';
    this.soloMias = false;
    this.fechaDesde = '';
    this.fechaHasta = '';
    this.recargarDesdeInicio();
  }

  get hayFiltros(): boolean {
    return !!(this.texto || this.resultado || this.soloMias || this.fechaDesde || this.fechaHasta);
  }

  private recargarDesdeInicio(): void {
    this.pagina = 0;
    this.cargar();
  }

  // ---------------- paginado ----------------

  irAPagina(p: number): void {
    if (!this.datos || p < 0 || p >= this.datos.totalPaginas || p === this.pagina) {
      return;
    }
    this.pagina = p;
    this.cargar();
  }

  get paginasVisibles(): (number | null)[] {
    const total = this.datos?.totalPaginas ?? 0;
    const paginas: (number | null)[] = [];
    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - this.pagina) <= 1) {
        paginas.push(i);
      } else if (paginas[paginas.length - 1] !== null) {
        paginas.push(null);
      }
    }
    return paginas;
  }

  get rangoMostrado(): string {
    if (!this.datos || this.datos.totalElementos === 0) {
      return '';
    }
    const desde = this.datos.pagina * this.datos.tamanio + 1;
    return `Mostrando ${desde}–${desde + this.datos.filas.length - 1} de ${this.datos.totalElementos}`;
  }

  // ---------------- textos ----------------

  /** 5 h, 2.3 días… */
  duracion(horas?: number | null): string {
    if (horas === null || horas === undefined) {
      return '—';
    }
    if (horas < 1) {
      return `${Math.max(1, Math.round(horas * 60))} min`;
    }
    if (horas < 48) {
      return `${Math.round(horas)} h`;
    }
    return `${(horas / 24).toFixed(1)} días`;
  }

  porcentaje(parte: number, total: number): number {
    return total > 0 ? Math.round((parte / total) * 100) : 0;
  }

  // ---------------- detalle y exportación ----------------

  verDetalle(fila: HistorialFila): void {
    this.revisionService.detalle(fila.numeroFolio).subscribe({
      next: (detalle) => {
        this.detalleSeleccionado = detalle;
        this.mostrarModalDetalle = true;
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('No se pudo cargar el detalle de la queja.'),
    });
  }

  cerrarDetalle(): void {
    this.mostrarModalDetalle = false;
    this.detalleSeleccionado = null;
  }

  exportar(): void {
    this.exportando = true;
    this.historialService.exportar(this.texto, this.resultado, this.fechaDesde, this.fechaHasta).subscribe({
      next: (blob) => {
        this.exportando = false;
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'historial-tramites.xlsx';
        enlace.click();
        URL.revokeObjectURL(url);
        this.cdr.detectChanges();
      },
      error: () => {
        this.exportando = false;
        this.toast.error('No se pudo exportar el reporte.');
        this.cdr.detectChanges();
      },
    });
  }

  private cargar(): void {
    this.cargando = true;
    this.historialService
      .pagina(this.texto, this.resultado, this.fechaDesde, this.fechaHasta, this.soloMias, this.pagina, TAMANIO_PAGINA)
      .subscribe({
        next: (datos) => {
          this.datos = datos;
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.cargando = false;
          this.toast.error('No se pudo cargar el historial.');
          this.cdr.detectChanges();
        },
      });
  }
}
