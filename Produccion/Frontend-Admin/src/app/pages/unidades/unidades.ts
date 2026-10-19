import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { PERIODOS, TablaUnidades, variacion } from '../../core/models/admin.models';
import { Barras, SerieBarras } from '../../shared/panel/barras';
import { Dona, SegmentoDona, pctTexto } from '../../shared/panel/dona';
import { UnidadesTabla } from '../../shared/panel/unidades-tabla';

const COLOR_TIPO: Record<string, string> = {
  ESCUELA_SUPERIOR: '#7a1f45',
  MEDIO_SUPERIOR: '#e46a9a',
  INVESTIGACION: '#8e44ad',
  UNIDAD_PROFESIONAL: '#2f6fed',
  OTROS: '#c4b5f0',
};

/** Quejas por dependencia o unidad académica (diseño 2026-10-18). */
@Component({
  selector: 'app-unidades',
  standalone: true,
  imports: [FormsModule, RouterLink, Barras, Dona, UnidadesTabla],
  templateUrl: './unidades.html',
  styleUrl: './unidades.scss',
})
export class Unidades implements OnInit {
  @ViewChild(UnidadesTabla) tabla?: UnidadesTabla;

  readonly periodos = PERIODOS;
  readonly pctTexto = pctTexto;
  dias = 365;
  resumen: TablaUnidades | null = null;
  cargando = true;

  constructor(
    private seguimiento: SeguimientoService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = !this.resumen;
    this.seguimiento.tablaUnidades({}, this.dias, 0, 1).subscribe({
      next: (r) => { this.resumen = r; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.toast.error('No se pudo cargar el resumen por unidad.'); this.cdr.detectChanges(); },
    });
  }

  get etiquetaPeriodo(): string {
    return this.dias === 0 ? 'en todo el historial' : 'en el periodo seleccionado';
  }

  get deltaUnidades(): number | null {
    return this.resumen ? variacion(this.resumen.resumen.unidadesConQueja, this.resumen.resumen.unidadesAnterior) : null;
  }

  get deltaQuejas(): number | null {
    return this.resumen ? variacion(this.resumen.resumen.totalQuejas, this.resumen.resumen.totalAnterior) : null;
  }

  get etiquetasTop(): string[] {
    return (this.resumen?.top ?? []).map((u) => (u.siglas === u.clave && u.nombre ? u.nombre : u.siglas));
  }

  get seriesTop(): SerieBarras[] {
    return [{ nombre: 'Quejas', color: '#7a1f45', valores: (this.resumen?.top ?? []).map((u) => u.total) }];
  }

  get segmentosTipo(): SegmentoDona[] {
    return (this.resumen?.porTipo ?? []).map((t) => ({ etiqueta: t.etiqueta, valor: t.total, color: COLOR_TIPO[t.clave] ?? '#999' }));
  }

  abrirTop(i: number): void {
    const u = this.resumen?.top[i];
    if (u) {
      this.router.navigate(['/quejas'], { queryParams: { unidad: u.clave } });
    }
  }

  exportar(): void {
    this.tabla?.exportar();
  }
}
