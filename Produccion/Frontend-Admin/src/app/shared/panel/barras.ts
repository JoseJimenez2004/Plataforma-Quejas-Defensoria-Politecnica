import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface SerieBarras {
  nombre: string;
  color: string;
  valores: number[];
}

/**
 * Barras verticales en SVG con eje Y, cuadrícula y valor encima de cada barra. Con varias
 * series las apila. {@code resaltarUltima} pinta la última barra en el color fuerte y las demás
 * en tono claro (como "Evolución de quejas" del diseño); {@code degradado} va de fuerte a claro.
 */
@Component({
  selector: 'app-barras',
  standalone: true,
  template: `
    <svg [attr.viewBox]="'0 0 ' + ancho + ' ' + alto" preserveAspectRatio="xMidYMid meet" class="barras" role="img"
         [attr.aria-label]="descripcion">
      @for (t of marcas; track t) {
        <line [attr.x1]="izq" [attr.x2]="ancho - 6" [attr.y1]="y(t)" [attr.y2]="y(t)" stroke="#eceef1" />
        <text [attr.x]="izq - 8" [attr.y]="y(t) + 4" text-anchor="end" class="eje">{{ t }}</text>
      }
      @for (e of etiquetas; track $index; let i = $index) {
        <g class="grupo" [class.clic]="clicable" (click)="clicable && seleccionar.emit(i)">
          <rect [attr.x]="xBarra(i) - 4" [attr.y]="arriba" [attr.width]="anchoBarra + 8" [attr.height]="base - arriba" fill="transparent" />
          @for (s of pilas(i); track s.nombre) {
            <rect [attr.x]="xBarra(i)" [attr.y]="s.y" [attr.width]="anchoBarra" [attr.height]="s.h" [attr.fill]="s.color" rx="3">
              <title>{{ e }} · {{ s.nombre }}: {{ s.valor }}</title>
            </rect>
          }
          @if (mostrarValores && totalDe(i) > 0) {
            <text [attr.x]="xBarra(i) + anchoBarra / 2" [attr.y]="y(totalDe(i)) - 6" text-anchor="middle"
                  class="valor" [class.fuerte]="resaltarUltima && i === etiquetas.length - 1">{{ totalDe(i) }}</text>
          }
          @for (linea of partir(e); track $index; let j = $index) {
            <text [attr.x]="xBarra(i) + anchoBarra / 2" [attr.y]="base + 18 + j * 14" text-anchor="middle" class="eje">{{ linea }}</text>
          }
        </g>
      }
    </svg>
  `,
  styles: [`
    :host { display: block; width: 100%; }
    .barras { width: 100%; height: auto; display: block; overflow: visible; }
    .eje { font-size: 12px; fill: #5c6370; }
    .valor { font-size: 12.5px; font-weight: 800; fill: #333; }
    .valor.fuerte { font-size: 14px; fill: #1a1a1a; }
    .grupo.clic { cursor: pointer; }
    .grupo.clic:hover rect:not([fill=transparent]) { opacity: 0.85; }
  `],
})
export class Barras {
  @Input() etiquetas: string[] = [];
  @Input() series: SerieBarras[] = [];
  @Input() alto = 230;
  @Input() ancho = 720;
  @Input() resaltarUltima = false;
  @Input() degradado = false;
  @Input() mostrarValores = true;
  @Input() clicable = false;
  @Input() descripcion = 'Gráfica de barras';
  /** Si las etiquetas son largas, se parten en dos renglones por espacio. */
  @Input() dosRenglones = false;
  @Output() seleccionar = new EventEmitter<number>();

  readonly izq = 40;
  readonly arriba = 18;

  get base(): number {
    return this.alto - (this.dosRenglones ? 40 : 26);
  }

  get maximo(): number {
    const m = Math.max(1, ...this.etiquetas.map((_, i) => this.totalDe(i)));
    const paso = this.paso(m);
    return Math.ceil(m / paso) * paso;
  }

  get marcas(): number[] {
    const paso = this.paso(this.maximo);
    const r: number[] = [];
    for (let v = 0; v <= this.maximo; v += paso) {
      r.push(v);
    }
    return r;
  }

  get anchoBarra(): number {
    const hueco = (this.ancho - this.izq - 10) / Math.max(1, this.etiquetas.length);
    return Math.max(8, Math.min(56, hueco * 0.62));
  }

  xBarra(i: number): number {
    const hueco = (this.ancho - this.izq - 10) / Math.max(1, this.etiquetas.length);
    return this.izq + hueco * i + (hueco - this.anchoBarra) / 2;
  }

  y(v: number): number {
    return this.base - (v / this.maximo) * (this.base - this.arriba);
  }

  totalDe(i: number): number {
    return this.series.reduce((s, x) => s + (x.valores[i] ?? 0), 0);
  }

  pilas(i: number): { nombre: string; color: string; valor: number; y: number; h: number }[] {
    let acumulado = 0;
    return this.series.map((s, k) => {
      const valor = s.valores[i] ?? 0;
      const y0 = this.y(acumulado);
      acumulado += valor;
      const y1 = this.y(acumulado);
      return { nombre: s.nombre, color: this.color(s, k, i), valor, y: y1, h: Math.max(0, y0 - y1) };
    }).filter((p) => p.valor > 0);
  }

  partir(e: string): string[] {
    if (!this.dosRenglones || e.length <= 10 || !e.includes(' ')) {
      return [e];
    }
    const i = e.lastIndexOf(' ');
    return [e.slice(0, i), e.slice(i + 1)];
  }

  private color(s: SerieBarras, k: number, i: number): string {
    if (this.series.length > 1) {
      return s.color;
    }
    if (this.resaltarUltima) {
      return i === this.etiquetas.length - 1 ? s.color : '#ecd3dd';
    }
    if (this.degradado) {
      const t = this.etiquetas.length <= 1 ? 0 : i / (this.etiquetas.length - 1);
      return mezclar(s.color, '#e8c6d4', t);
    }
    return s.color;
  }

  private paso(m: number): number {
    if (m <= 5) return 1;
    if (m <= 10) return 2;
    if (m <= 25) return 5;
    if (m <= 50) return 10;
    if (m <= 120) return 20;
    if (m <= 250) return 50;
    return Math.pow(10, Math.floor(Math.log10(m)));
  }
}

function mezclar(a: string, b: string, t: number): string {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return '#' + pa.map((v, i) => Math.round(v + (pb[i] - v) * t).toString(16).padStart(2, '0')).join('');
}
