import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { CasoAtencion, FiltroCasos, FiltrosSeg, PaginaCasos, ReglaCaso, UnidadSeg } from '../../core/models/admin.models';
import { Paginador } from './paginador';

/** Rangos del filtro "Días desde recepción". */
export const RANGOS_DIAS: { clave: string; etiqueta: string; min: number | null; max: number | null }[] = [
  { clave: '0-3', etiqueta: '0 a 3 días', min: 0, max: 3 },
  { clave: '4-7', etiqueta: '4 a 7 días', min: 4, max: 7 },
  { clave: '8-15', etiqueta: '8 a 15 días', min: 8, max: 15 },
  { clave: '16-30', etiqueta: '16 a 30 días', min: 16, max: 30 },
  { clave: '31+', etiqueta: 'Más de 30 días', min: 31, max: null },
];

const TAMANIO = 8;

/**
 * "Casos que requieren atención" completo: buscador, filtros por motivo, etapa, unidad,
 * responsable y días, tabla paginada y "Ver expediente". Cada regla explica en su tooltip
 * por qué la queja aparece aquí.
 */
@Component({
  selector: 'app-atencion-tabla',
  standalone: true,
  imports: [FormsModule, Paginador],
  templateUrl: './atencion-tabla.html',
})
export class AtencionTabla implements OnInit, OnDestroy {
  datos: PaginaCasos | null = null;
  cargando = true;
  pagina = 0;
  texto = '';
  filtro: FiltroCasos = {};
  rango = '';
  readonly rangos = RANGOS_DIAS;

  opciones: FiltrosSeg | null = null;
  unidades: UnidadSeg[] = [];
  private espera: ReturnType<typeof setTimeout> | null = null;

  constructor(private seguimiento: SeguimientoService, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.seguimiento.filtros().subscribe((f) => { this.opciones = f; this.cdr.detectChanges(); });
    this.seguimiento.unidades().subscribe((u) => { this.unidades = u; this.cdr.detectChanges(); });
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.espera) {
      clearTimeout(this.espera);
    }
  }

  get reglas(): ReglaCaso[] {
    return this.datos?.reglas ?? [];
  }

  get responsables(): string[] {
    const r = new Set<string>();
    (this.opciones?.recepcionistas ?? []).forEach((x) => r.add(x.nombre));
    (this.opciones?.abogados ?? []).forEach((x) => r.add(x));
    return [...r].sort((a, b) => a.localeCompare(b));
  }

  descripcion(clave: string): string {
    return this.reglas.find((r) => r.clave === clave)?.descripcion ?? '';
  }

  buscar(): void {
    if (this.espera) {
      clearTimeout(this.espera);
    }
    this.espera = setTimeout(() => this.aplicar(), 350);
  }

  aplicar(): void {
    const r = this.rangos.find((x) => x.clave === this.rango);
    this.filtro = { ...this.filtro, q: this.texto.trim(), diasMin: r?.min ?? null, diasMax: r?.max ?? null };
    this.pagina = 0;
    this.cargar();
  }

  limpiar(): void {
    this.texto = '';
    this.rango = '';
    this.filtro = {};
    this.pagina = 0;
    this.cargar();
  }

  irA(p: number): void {
    this.pagina = p;
    this.cargar();
  }

  ver(c: CasoAtencion): void {
    this.router.navigate(['/quejas', c.folio]);
  }

  private cargar(): void {
    this.cargando = true;
    this.seguimiento.atencion(this.filtro, this.pagina, TAMANIO).subscribe({
      next: (d) => { this.datos = d; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.cdr.detectChanges(); },
    });
  }
}
