import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { FilaQuejaSeg, FiltroQuejasSeg, FiltrosSeg, PaginaQuejasSeg, UnidadSeg } from '../../core/models/admin.models';
import { Paginador } from '../../shared/panel/paginador';
import { descargarCsv, fechaCorta, fechaHora } from '../../shared/panel/csv';

const TAMANIO = 10;

/** Filtros que viven en la URL (los del formulario y los que llegan desde el panel). */
const CLAVES: (keyof FiltroQuejasSeg)[] = ['q', 'etapa', 'unidad', 'abogado', 'recepcionista', 'desde', 'hasta', 'estado',
  'prioridad', 'estatus', 'mes', 'motivo', 'orden'];

const ETIQUETA_MOTIVO: Record<string, string> = {
  NOMBRE: 'Nombre no coincide', IDENTIFICACION: 'Sin identificación válida', BOLETA: 'Boleta incorrecta',
  EMPLEADO: 'No. de empleado incorrecto', PLAZO: 'Fuera de plazo', DENUNCIADO: 'Datos del señalado insuficientes', OTRO: 'Otro motivo',
};

/**
 * Todas las quejas del sistema (diseño 2026-10-18). Los filtros se editan en el formulario y
 * se aplican con "Aplicar filtros"; quedan en la URL para que cada número del panel abra
 * esta pantalla ya filtrada y el botón "atrás" funcione.
 */
@Component({
  selector: 'app-quejas',
  standalone: true,
  imports: [FormsModule, Paginador],
  templateUrl: './quejas.html',
  styleUrl: './quejas.scss',
})
export class Quejas implements OnInit, OnDestroy {
  /** Lo aplicado (lo que está en la URL). */
  filtro: FiltroQuejasSeg = {};
  /** Lo que se está editando en el formulario. */
  borrador: FiltroQuejasSeg = {};
  pagina = 0;
  datos: PaginaQuejasSeg | null = null;
  cargando = true;
  vista: 'lista' | 'tarjetas' = 'lista';
  seleccion = new Set<string>();
  menuAbierto: string | null = null;
  exportando = false;

  opciones: FiltrosSeg | null = null;
  unidades: UnidadSeg[] = [];
  readonly fechaCorta = fechaCorta;
  readonly fechaHora = fechaHora;

  private sub?: Subscription;

  constructor(
    private seguimiento: SeguimientoService,
    private route: ActivatedRoute,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    try {
      this.vista = localStorage.getItem('ddp_admin_vista_quejas') === 'tarjetas' ? 'tarjetas' : 'lista';
    } catch {
      this.vista = 'lista';
    }
    this.seguimiento.filtros().subscribe({ next: (f) => { this.opciones = f; this.cdr.detectChanges(); } });
    this.seguimiento.unidades().subscribe({ next: (u) => { this.unidades = u; this.cdr.detectChanges(); } });
    this.sub = this.route.queryParamMap.subscribe((p) => {
      const f: FiltroQuejasSeg = {};
      for (const k of CLAVES) {
        const v = p.get(k);
        if (v) {
          f[k] = v;
        }
      }
      this.filtro = f;
      this.borrador = { ...f };
      this.pagina = Number(p.get('pagina') ?? 0) || 0;
      this.seleccion.clear();
      this.cargar();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  @HostListener('document:click')
  cerrarMenu(): void {
    this.menuAbierto = null;
  }

  // ------------------------------------------------------------------ filtros

  aplicar(): void {
    this.navegar({ ...this.borrador, orden: this.filtro.orden });
  }

  limpiar(): void {
    this.navegar({ orden: this.filtro.orden });
  }

  quitar(clave: keyof FiltroQuejasSeg): void {
    const f = { ...this.filtro };
    delete f[clave];
    this.navegar(f);
  }

  ordenar(orden: string): void {
    this.navegar({ ...this.filtro, orden: orden === 'reciente' ? undefined : orden });
  }

  irAPagina(p: number): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { pagina: p || null }, queryParamsHandling: 'merge' });
  }

  private navegar(f: FiltroQuejasSeg): void {
    const query: Record<string, string | null> = {};
    for (const k of CLAVES) {
      query[k] = f[k] ? String(f[k]).trim() || null : null;
    }
    query['pagina'] = null;
    this.router.navigate([], { relativeTo: this.route, queryParams: query });
  }

  /** Filtros que llegan desde el panel y no tienen selector propio: se muestran como chips. */
  get chips(): { clave: keyof FiltroQuejasSeg; texto: string }[] {
    const r: { clave: keyof FiltroQuejasSeg; texto: string }[] = [];
    if (this.filtro.estatus) {
      r.push({ clave: 'estatus', texto: 'Estatus: ' + this.filtro.estatus.replace(/_/g, ' ').toLowerCase() });
    }
    if (this.filtro.mes) {
      r.push({ clave: 'mes', texto: 'Mes: ' + this.filtro.mes });
    }
    if (this.filtro.motivo) {
      r.push({ clave: 'motivo', texto: 'Motivo de rechazo: ' + (ETIQUETA_MOTIVO[this.filtro.motivo] ?? this.filtro.motivo) });
    }
    return r;
  }

  get hayFiltros(): boolean {
    return CLAVES.some((k) => k !== 'orden' && !!this.filtro[k]);
  }

  // ------------------------------------------------------------------ vista y selección

  cambiarVista(v: 'lista' | 'tarjetas'): void {
    this.vista = v;
    try {
      localStorage.setItem('ddp_admin_vista_quejas', v);
    } catch {
      /* sin almacenamiento: solo dura esta visita */
    }
  }

  get todasMarcadas(): boolean {
    return !!this.datos?.filas.length && this.datos.filas.every((f) => this.seleccion.has(f.folio));
  }

  marcarTodas(marcar: boolean): void {
    (this.datos?.filas ?? []).forEach((f) => (marcar ? this.seleccion.add(f.folio) : this.seleccion.delete(f.folio)));
  }

  marcar(folio: string, marcar: boolean): void {
    if (marcar) {
      this.seleccion.add(folio);
    } else {
      this.seleccion.delete(folio);
    }
  }

  alternarMenu(folio: string, e: Event): void {
    e.stopPropagation();
    this.menuAbierto = this.menuAbierto === folio ? null : folio;
  }

  abrir(folio: string): void {
    this.router.navigate(['/quejas', folio]);
  }

  copiarFolio(folio: string): void {
    this.menuAbierto = null;
    navigator.clipboard?.writeText(folio).then(
      () => this.toast.exito(`Folio ${folio} copiado.`),
      () => this.toast.advertencia('No se pudo copiar el folio.'),
    );
  }

  filtrarPor(f: FiltroQuejasSeg): void {
    this.menuAbierto = null;
    this.navegar({ ...f, orden: this.filtro.orden });
  }

  // ------------------------------------------------------------------ exportar

  /** Exporta las seleccionadas, o todo el resultado del filtro si no hay selección. */
  exportar(): void {
    this.exportando = true;
    this.seguimiento.quejas(this.filtro, 0, 5000).subscribe({
      next: (d) => {
        const filas = this.seleccion.size ? d.filas.filter((f) => this.seleccion.has(f.folio)) : d.filas;
        descargarCsv('quejas', ['Folio', 'Fecha de llegada', 'Quejoso', 'Unidad / dependencia', 'Etapa', 'Estado', 'Prioridad',
          'Abogado responsable', 'Recepcionista', 'Días desde la llegada', 'Origen'],
        filas.map((f) => [f.folio, fechaHora(f.fecha), f.quejoso, f.unidadNombre || f.unidadClave, f.estatusEtiqueta, f.estadoEtiqueta,
          f.prioridad ?? '', f.abogado ?? '', f.recepcionista ?? '', f.dias, f.origen]));
        this.exportando = false;
        this.toast.exito(`Se exportaron ${filas.length} quejas.`);
        this.cdr.detectChanges();
      },
      error: () => {
        this.exportando = false;
        this.toast.error('No se pudo exportar.');
        this.cdr.detectChanges();
      },
    });
  }

  // ------------------------------------------------------------------ utilidades de la tabla

  recepcionistaDe(f: FilaQuejaSeg): string {
    if (f.recepcionista) {
      return f.recepcionista;
    }
    return f.etapa === 'RECEPCION' ? 'Nadie la ha tomado' : '—';
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
