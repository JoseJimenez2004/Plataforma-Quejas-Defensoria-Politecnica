import { Component, EventEmitter, Input, Output } from '@angular/core';

/** "Mostrando 1–10 de 182 resultados" + botones ‹ 1 2 3 … 19 ›. Páginas base 0. */
@Component({
  selector: 'app-paginador',
  standalone: true,
  template: `
    <div class="seg-paginado">
      <span>{{ rango }}</span>
      @if (totalPaginas > 1) {
        <nav aria-label="Paginación">
          <button type="button" (click)="ir(pagina - 1)" [disabled]="pagina === 0" aria-label="Anterior">‹</button>
          @for (p of visibles; track $index) {
            @if (p === null) { <span class="puntos">…</span> }
            @else { <button type="button" [class.actual]="p === pagina" (click)="ir(p)">{{ p + 1 }}</button> }
          }
          <button type="button" (click)="ir(pagina + 1)" [disabled]="pagina >= totalPaginas - 1" aria-label="Siguiente">›</button>
        </nav>
      }
    </div>
  `,
})
export class Paginador {
  @Input() pagina = 0;
  @Input() tamanio = 10;
  @Input() totalElementos = 0;
  @Input() enPagina = 0;
  @Input() sustantivo = 'resultados';
  @Output() cambiar = new EventEmitter<number>();

  get totalPaginas(): number {
    return Math.ceil(this.totalElementos / Math.max(1, this.tamanio));
  }

  get rango(): string {
    if (!this.totalElementos) {
      return `0 ${this.sustantivo}`;
    }
    const desde = this.pagina * this.tamanio + 1;
    return `Mostrando ${desde}–${desde + Math.max(this.enPagina, 1) - 1} de ${this.totalElementos} ${this.sustantivo}`;
  }

  get visibles(): (number | null)[] {
    const total = this.totalPaginas;
    const r: (number | null)[] = [];
    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - this.pagina) <= 2 || (this.pagina < 3 && i < 5)) {
        r.push(i);
      } else if (r[r.length - 1] !== null) {
        r.push(null);
      }
    }
    return r;
  }

  ir(p: number): void {
    if (p >= 0 && p < this.totalPaginas && p !== this.pagina) {
      this.cambiar.emit(p);
    }
  }
}
