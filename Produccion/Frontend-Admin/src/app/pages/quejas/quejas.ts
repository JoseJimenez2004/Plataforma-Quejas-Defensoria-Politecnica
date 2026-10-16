import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { FiltroQuejasSeg, FiltrosSeg, PaginaQuejasSeg, UnidadSeg } from '../../core/models/admin.models';

const TAMANIO = 15;

/**
 * Explorador de quejas de la Defensora (2026-10-16). Los filtros viven en la URL
 * (?etapa=…&unidad=…&abogado=…), así cada número del panel general abre esta pantalla ya
 * filtrada y el botón "atrás" del navegador funciona.
 */
@Component({
  selector: 'app-quejas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './quejas.html',
  styleUrl: './quejas.scss',
})
export class Quejas implements OnInit, OnDestroy {
  filtro: FiltroQuejasSeg = {};
  texto = '';
  pagina = 0;
  datos: PaginaQuejasSeg | null = null;
  cargando = true;

  opciones: FiltrosSeg | null = null;
  unidades: UnidadSeg[] = [];

  private sub?: Subscription;
  private busquedaId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private seguimiento: SeguimientoService,
    private route: ActivatedRoute,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.seguimiento.filtros().subscribe({ next: (f) => { this.opciones = f; this.cdr.detectChanges(); } });
    this.seguimiento.unidades().subscribe({ next: (u) => { this.unidades = u; this.cdr.detectChanges(); } });
    this.sub = this.route.queryParamMap.subscribe((p) => {
      this.filtro = {
        etapa: p.get('etapa') ?? '',
        estatus: p.get('estatus') ?? '',
        unidad: p.get('unidad') ?? '',
        abogado: p.get('abogado') ?? '',
        recepcionista: p.get('recepcionista') ?? '',
        mes: p.get('mes') ?? '',
        q: p.get('q') ?? '',
      };
      this.texto = this.filtro.q ?? '';
      this.pagina = Number(p.get('pagina') ?? 0) || 0;
      this.cargar();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
  }

  /** Cambia un filtro (o varios) y lo refleja en la URL; vuelve a la página 1. */
  aplicar(cambios: Partial<FiltroQuejasSeg>): void {
    const siguiente = { ...this.filtro, ...cambios };
    const query: Record<string, string | null> = {};
    for (const [k, v] of Object.entries(siguiente)) {
      query[k] = v ? String(v) : null;
    }
    query['pagina'] = null;
    this.router.navigate([], { relativeTo: this.route, queryParams: query });
  }

  buscar(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
    this.busquedaId = setTimeout(() => this.aplicar({ q: this.texto.trim() }), 350);
  }

  limpiar(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  irAPagina(p: number): void {
    if (!this.datos || p < 0 || p >= this.datos.totalPaginas) {
      return;
    }
    this.router.navigate([], { relativeTo: this.route, queryParams: { pagina: p || null }, queryParamsHandling: 'merge' });
  }

  get hayFiltros(): boolean {
    return Object.values(this.filtro).some((v) => !!v);
  }

  /** Etiquetas de los filtros activos que no tienen selector visible (estatus y mes desde el panel). */
  get etiquetaEstatus(): string {
    return this.filtro.estatus ? this.filtro.estatus.replace(/_/g, ' ').toLowerCase() : '';
  }

  get paginasVisibles(): (number | null)[] {
    const total = this.datos?.totalPaginas ?? 0;
    const r: (number | null)[] = [];
    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - this.pagina) <= 1) {
        r.push(i);
      } else if (r[r.length - 1] !== null) {
        r.push(null);
      }
    }
    return r;
  }

  get rango(): string {
    if (!this.datos || this.datos.totalElementos === 0) {
      return '';
    }
    const desde = this.datos.pagina * this.datos.tamanio + 1;
    return `Mostrando ${desde}–${desde + this.datos.filas.length - 1} de ${this.datos.totalElementos}`;
  }

  nombreUnidad(clave?: string | null): string {
    const u = this.unidades.find((x) => x.clave === clave);
    return u ? (u.abreviatura || u.nombre || u.clave) : (clave ?? '');
  }

  abrir(folio: string): void {
    this.router.navigate(['/quejas', folio]);
  }

  private cargar(): void {
    this.cargando = true;
    this.seguimiento.quejas(this.filtro, this.pagina, TAMANIO).subscribe({
      next: (d) => {
        this.datos = d;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudieron cargar las quejas.');
        this.cdr.detectChanges();
      },
    });
  }
}
