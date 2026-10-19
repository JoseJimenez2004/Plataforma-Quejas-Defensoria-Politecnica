import { Component, Input } from '@angular/core';

export interface SegmentoDona {
  etiqueta: string;
  valor: number;
  color: string;
}

/**
 * Gráfica de dona en SVG (sin librerías). Al centro, el total y una leyenda corta. Cada
 * segmento lleva su <title> para el tooltip nativo.
 */
@Component({
  selector: 'app-dona',
  standalone: true,
  template: `
    <svg [attr.viewBox]="'0 0 ' + tam + ' ' + tam" [style.width.px]="tam" [style.height.px]="tam" role="img"
         [attr.aria-label]="etiquetaCentro + ': ' + total">
      <circle [attr.cx]="c" [attr.cy]="c" [attr.r]="r" fill="none" stroke="#eceef1" [attr.stroke-width]="grosor" />
      @for (s of arcos; track s.etiqueta) {
        <circle [attr.cx]="c" [attr.cy]="c" [attr.r]="r" fill="none" [attr.stroke]="s.color" [attr.stroke-width]="grosor"
                [attr.stroke-dasharray]="s.largo + ' ' + (circ - s.largo)" [attr.stroke-dashoffset]="-s.inicio"
                [attr.transform]="'rotate(-90 ' + c + ' ' + c + ')'">
          <title>{{ s.etiqueta }}: {{ s.valor }}</title>
        </circle>
      }
      <text [attr.x]="c" [attr.y]="c - 2" text-anchor="middle" class="dona-num" [style.font-size.px]="tam * 0.17">{{ total }}</text>
      <text [attr.x]="c" [attr.y]="c + tam * 0.11" text-anchor="middle" class="dona-txt" [style.font-size.px]="tam * 0.075">{{ etiquetaCentro }}</text>
    </svg>
  `,
  styles: [`
    :host { display: inline-block; line-height: 0; }
    .dona-num { font-weight: 900; fill: #1a1a1a; }
    .dona-txt { fill: #555; }
  `],
})
export class Dona {
  @Input() segmentos: SegmentoDona[] = [];
  @Input() etiquetaCentro = 'Quejas';
  @Input() tam = 190;
  @Input() grosor = 26;

  get c(): number {
    return this.tam / 2;
  }

  get r(): number {
    return (this.tam - this.grosor) / 2 - 2;
  }

  get circ(): number {
    return 2 * Math.PI * this.r;
  }

  get total(): number {
    return this.segmentos.reduce((s, x) => s + x.valor, 0);
  }

  get arcos(): (SegmentoDona & { inicio: number; largo: number })[] {
    const total = this.total || 1;
    let acumulado = 0;
    const separacion = this.segmentos.filter((s) => s.valor > 0).length > 1 ? 1.5 : 0;
    return this.segmentos
      .filter((s) => s.valor > 0)
      .map((s) => {
        const largo = Math.max(0.5, (s.valor / total) * this.circ - separacion);
        const arco = { ...s, inicio: acumulado, largo };
        acumulado += (s.valor / total) * this.circ;
        return arco;
      });
  }
}

/** Porcentaje legible: "<1%" cuando es mayor que cero pero no llega a 1. */
export function pctTexto(parte: number, total: number): string {
  if (!total || !parte) {
    return '0%';
  }
  const p = (parte / total) * 100;
  return p < 1 ? '<1%' : `${Math.round(p)}%`;
}
