import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { CargaTrabajo, MiembroCarga, PERIODOS } from '../../core/models/admin.models';
import { Barras, SerieBarras } from '../../shared/panel/barras';
import { Dona, SegmentoDona, pctTexto } from '../../shared/panel/dona';
import { CargaTabla } from '../../shared/panel/carga-tabla';
import { COLOR_ETAPA, ETIQUETA_ETAPA_CORTA as ETIQUETA_ETAPA } from '../../shared/panel/colores';

/**
 * Carga de trabajo del personal (diseño 2026-10-18): cuántas quejas tiene y resolvió cada
 * recepcionista y cada abogado en el periodo, tiempo promedio de atención y reparto por etapa.
 */
@Component({
  selector: 'app-carga-trabajo',
  standalone: true,
  imports: [FormsModule, RouterLink, Barras, Dona, CargaTabla],
  templateUrl: './carga-trabajo.html',
  styleUrl: './carga-trabajo.scss',
})
export class CargaTrabajoPagina implements OnInit {
  @ViewChild(CargaTabla) tabla?: CargaTabla;

  readonly periodos = PERIODOS;
  readonly pctTexto = pctTexto;
  dias = 30;
  datos: CargaTrabajo | null = null;
  cargando = true;

  constructor(private seguimiento: SeguimientoService, private toast: ToastService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = !this.datos;
    this.seguimiento.carga(this.dias).subscribe({
      next: (d) => { this.datos = d; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.toast.error('No se pudo cargar la carga de trabajo.'); this.cdr.detectChanges(); },
    });
  }

  get pctTramite(): string {
    return this.datos ? pctTexto(this.datos.kpis.enTramite, this.datos.kpis.totalQuejas) : '0%';
  }

  get pctAtendidas(): string {
    return this.datos ? pctTexto(this.datos.kpis.atendidas, this.datos.kpis.totalQuejas) : '0%';
  }

  /** Cambio del tiempo promedio: negativo es bueno (se atiende más rápido). */
  get deltaTiempo(): number | null {
    const k = this.datos?.kpis;
    if (!k || k.diasPromedio == null || !k.diasPromedioAnterior) {
      return null;
    }
    return Math.round(((k.diasPromedio - k.diasPromedioAnterior) / k.diasPromedioAnterior) * 100);
  }

  /** Las 6 personas con más carga y el resto agrupado en "Otros". */
  private get top(): { etiqueta: string; tramite: number; atendidas: number }[] {
    const todos: MiembroCarga[] = [...(this.datos?.recepcionistas ?? []), ...(this.datos?.abogados ?? [])]
      .filter((m) => m.carga > 0 || m.atendidas > 0)
      .sort((a, b) => (b.enTramite + b.atendidas) - (a.enTramite + a.atendidas));
    const valor = (m: MiembroCarga) => ({
      tramite: m.rol === 'RECEPCIONISTA' ? m.abiertas : m.enTramite,
      atendidas: m.rol === 'RECEPCIONISTA' ? m.atendidas : m.cerradas,
    });
    const r = todos.slice(0, 6).map((m) => ({ etiqueta: corto(m), ...valor(m) }));
    const resto = todos.slice(6);
    if (resto.length) {
      r.push({
        etiqueta: 'Otros',
        tramite: resto.reduce((s, m) => s + valor(m).tramite, 0),
        atendidas: resto.reduce((s, m) => s + valor(m).atendidas, 0),
      });
    }
    return r;
  }

  get etiquetasResp(): string[] {
    return this.top.map((t) => t.etiqueta);
  }

  get seriesResp(): SerieBarras[] {
    const t = this.top;
    return [
      { nombre: 'En trámite', color: '#7a1f45', valores: t.map((x) => x.tramite) },
      { nombre: 'Atendidas / concluidas', color: '#d98aab', valores: t.map((x) => x.atendidas) },
    ];
  }

  get segmentosEtapa(): SegmentoDona[] {
    return (this.datos?.etapas ?? []).map((e) => ({ etiqueta: ETIQUETA_ETAPA[e.clave] ?? e.etiqueta, valor: e.total, color: COLOR_ETAPA[e.clave] ?? '#999' }));
  }

  get totalEtapas(): number {
    return this.segmentosEtapa.reduce((s, x) => s + x.valor, 0);
  }

  exportar(): void {
    this.tabla?.exportar();
  }
}

/** "Lic. Alejandra Vega Morales" → "Alejandra Vega" + rol en segundo renglón. */
function corto(m: MiembroCarga): string {
  const partes = m.nombre.replace(/^(lic|dra?|mtr[oa]|ing)\.?\s+/i, '').split(/\s+/);
  const nombre = partes.length > 1 ? `${partes[0]} ${partes[1].charAt(0)}.` : partes[0];
  return `${nombre} ${m.rol === 'RECEPCIONISTA' ? '(Recep.)' : '(Abog.)'}`;
}
