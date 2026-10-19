import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthAdminService } from '../../core/services/auth-admin.service';
import { CargaTrabajo, EtapaSeg, FilaUnidad, PERIODOS, PanelDefensora, variacion } from '../../core/models/admin.models';
import { ModalPanel } from '../../shared/panel/modal-panel';
import { Barras, SerieBarras } from '../../shared/panel/barras';
import { Dona, SegmentoDona, pctTexto } from '../../shared/panel/dona';
import { AtencionTabla } from '../../shared/panel/atencion-tabla';
import { UnidadesTabla } from '../../shared/panel/unidades-tabla';
import { CargaTabla } from '../../shared/panel/carga-tabla';
import { MotivosTabla } from '../../shared/panel/motivos-tabla';
import { COLOR_ETAPA, ETIQUETA_ETAPA_CORTA as ETIQUETA_CORTA } from '../../shared/panel/colores';

type Ventana = 'atencion' | 'etapas' | 'unidades' | 'carga' | 'motivos' | null;

/**
 * Panel de la Defensora (diseño 2026-10-18): indicadores del periodo, casos que requieren
 * atención, dónde están las quejas, evolución mensual, unidades, carga del personal y motivos
 * de rechazo. Cada "Ver todas" abre una ventana con filtros; cada número lleva a sus quejas.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, ModalPanel, Barras, Dona, AtencionTabla, UnidadesTabla, CargaTabla, MotivosTabla],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  readonly periodos = PERIODOS;
  readonly colorEtapa = COLOR_ETAPA;
  readonly etiquetaCorta = ETIQUETA_CORTA;
  readonly pctTexto = pctTexto;

  panel: PanelDefensora | null = null;
  cargando = true;
  dias = 30;
  meses = 12;

  ventana: Ventana = null;
  carga: CargaTrabajo | null = null;

  readonly fechaHoy = (() => {
    const t = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date());
    return t.charAt(0).toUpperCase() + t.slice(1);
  })();

  constructor(
    private seguimiento: SeguimientoService,
    private auth: AuthAdminService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  get titulo(): string {
    return this.auth.usuarioActual()?.rol === 'DEFENSOR' ? 'Panel de la Defensora' : 'Panel general';
  }

  get etiquetaPeriodo(): string {
    return this.dias === 0 ? 'en todo el historial' : `en los últimos ${this.dias} días`;
  }

  cargar(): void {
    this.cargando = !this.panel;
    this.seguimiento.tablero(this.dias, this.meses).subscribe({
      next: (p) => {
        this.panel = p;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el panel general.');
        this.cdr.detectChanges();
      },
    });
  }

  // ------------------------------------------------------------------ indicadores

  get delta(): number | null {
    return this.panel ? variacion(this.panel.kpis.recibidasPeriodo, this.panel.kpis.recibidasAnterior) : null;
  }

  pctDe(n: number): number {
    const total = this.panel?.kpis.total ?? 0;
    return total ? Math.round((n / total) * 100) : 0;
  }

  irAQuejas(queryParams: Record<string, string>): void {
    this.router.navigate(['/quejas'], { queryParams });
  }

  // ------------------------------------------------------------------ etapas

  /** Las 4 tarjetas del diseño (Con el quejoso se muestra aparte, como nota). */
  get etapasTarjeta(): EtapaSeg[] {
    return (this.panel?.etapas ?? []).filter((e) => e.clave !== 'CON_QUEJOSO');
  }

  get etapaQuejoso(): EtapaSeg | undefined {
    return this.panel?.etapas.find((e) => e.clave === 'CON_QUEJOSO');
  }

  get totalEtapas(): number {
    return (this.panel?.etapas ?? []).reduce((s, e) => s + e.total, 0);
  }

  get segmentosEtapas(): SegmentoDona[] {
    return (this.panel?.etapas ?? []).map((e) => ({ etiqueta: ETIQUETA_CORTA[e.clave] ?? e.etiqueta, valor: e.total, color: COLOR_ETAPA[e.clave] ?? '#999' }));
  }

  // ------------------------------------------------------------------ evolución

  get etiquetasMes(): string[] {
    return (this.panel?.porMes ?? []).map((m) => m.etiqueta);
  }

  get seriesMes(): SerieBarras[] {
    return [{ nombre: 'Quejas recibidas', color: '#7a1f45', valores: (this.panel?.porMes ?? []).map((m) => m.recibidas) }];
  }

  cambiarMeses(): void {
    this.cargar();
  }

  abrirMes(i: number): void {
    const m = this.panel?.porMes[i];
    if (m) {
      this.irAQuejas({ mes: m.clave });
    }
  }

  // ------------------------------------------------------------------ unidades, carga y motivos

  get maxUnidad(): number {
    return Math.max(1, ...(this.panel?.unidades ?? []).map((u) => u.total));
  }

  get maxMotivo(): number {
    return Math.max(1, ...(this.panel?.motivos ?? []).map((m) => m.cantidad));
  }

  /** Siglas; si la dependencia no tiene, su nombre (p. ej. "Coordinación de Asesores" en vez de "DG.1"). */
  nombreCorto(u: FilaUnidad): string {
    return u.siglas === u.clave && u.nombre ? u.nombre : u.siglas;
  }

  abrir(v: Ventana): void {
    this.ventana = v;
    if (v === 'carga' && (!this.carga || this.carga.dias !== this.dias)) {
      this.seguimiento.carga(this.dias).subscribe((c) => { this.carga = c; this.cdr.detectChanges(); });
    }
  }

  cerrar(): void {
    this.ventana = null;
  }

  /** Desde la ventana de etapas: abre la lista de quejas de esa etapa. */
  verEtapa(clave: string): void {
    this.ventana = null;
    this.irAQuejas({ etapa: clave });
  }
}
