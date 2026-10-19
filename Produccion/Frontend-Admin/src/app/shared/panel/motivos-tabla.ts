import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { MotivoRechazo } from '../../core/models/admin.models';

/** Catálogo de motivos con los que Recepción regresa una queja, y cuántas veces se usó cada uno. */
@Component({
  selector: 'app-motivos-tabla',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="seg-filtros">
      <div class="seg-buscar ancho-2">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
        <input type="search" [(ngModel)]="filtro.q" (ngModelChange)="cargar()" placeholder="Buscar motivo…" aria-label="Buscar motivo" />
      </div>
      <label class="seg-campo">Categoría
        <select [(ngModel)]="filtro.categoria" (ngModelChange)="cargar()">
          <option [ngValue]="undefined">Todas</option>
          @for (c of categorias; track c) { <option [ngValue]="c">{{ c }}</option> }
        </select>
      </label>
      <label class="seg-campo">Desde <input type="date" [(ngModel)]="filtro.desde" (ngModelChange)="cargar()" /></label>
      <label class="seg-campo">Hasta <input type="date" [(ngModel)]="filtro.hasta" (ngModelChange)="cargar()" /></label>
      <button type="button" class="seg-btn-borde" (click)="filtro = {}; cargar()">Limpiar filtros</button>
    </div>
    <div class="seg-tabla-envoltura">
      <table class="seg-tabla">
        <thead><tr><th>#</th><th>Motivo</th><th>Categoría</th><th class="num">Cantidad</th><th>Porcentaje</th><th>Acción</th></tr></thead>
        <tbody>
          @for (m of motivos; track m.clave; let i = $index) {
            <tr>
              <td>{{ i + 1 }}</td>
              <td class="fuerte envolver">{{ m.motivo }}</td>
              <td>{{ m.categoria }}</td>
              <td class="num"><b>{{ m.cantidad }}</b></td>
              <td><span class="seg-carga"><span class="seg-barra"><span [style.width.%]="m.porcentaje"></span></span><b>{{ m.porcentaje }}%</b></span></td>
              <td><button type="button" class="seg-ver" [disabled]="!m.cantidad" (click)="ver(m)">Ver quejas</button></td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="seg-vacio">{{ cargando ? 'Cargando…' : 'Sin motivos con estos filtros.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <p class="seg-nota">El porcentaje es sobre las quejas regresadas con observaciones en el periodo. Una queja puede tener más de un motivo.</p>
  `,
})
export class MotivosTabla implements OnInit {
  motivos: MotivoRechazo[] = [];
  categorias: string[] = [];
  cargando = true;
  filtro: { q?: string; categoria?: string; desde?: string; hasta?: string } = {};

  constructor(private seguimiento: SeguimientoService, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.seguimiento.motivos(this.filtro).subscribe({
      next: (m) => {
        this.motivos = m;
        if (!this.categorias.length) {
          this.categorias = [...new Set(m.map((x) => x.categoria))];
        }
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => { this.cargando = false; this.cdr.detectChanges(); },
    });
  }

  ver(m: MotivoRechazo): void {
    this.router.navigate(['/quejas'], { queryParams: { motivo: m.clave } });
  }
}
