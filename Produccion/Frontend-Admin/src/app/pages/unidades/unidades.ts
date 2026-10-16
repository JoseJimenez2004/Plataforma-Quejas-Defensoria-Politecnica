import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { UnidadSeg } from '../../core/models/admin.models';

/** Quejas por dependencia o unidad académica: de la que más tiene a la que menos. */
@Component({
  selector: 'app-unidades',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './unidades.html',
  styleUrl: './unidades.scss',
})
export class Unidades implements OnInit, OnDestroy {
  unidades: UnidadSeg[] = [];
  texto = '';
  cargando = true;
  private busquedaId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private seguimiento: SeguimientoService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
  }

  buscar(): void {
    if (this.busquedaId !== null) {
      clearTimeout(this.busquedaId);
    }
    this.busquedaId = setTimeout(() => this.cargar(), 300);
  }

  get maximo(): number {
    return Math.max(1, ...this.unidades.map((u) => u.total));
  }

  get totalQuejas(): number {
    return this.unidades.reduce((s, u) => s + u.total, 0);
  }

  pct(n: number): number {
    return Math.max(n > 0 ? 2 : 0, Math.round((n / this.maximo) * 100));
  }

  abrir(u: UnidadSeg): void {
    this.router.navigate(['/quejas'], { queryParams: { unidad: u.clave } });
  }

  private cargar(): void {
    this.cargando = true;
    this.seguimiento.unidades(this.texto).subscribe({
      next: (u) => {
        this.unidades = u;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudieron cargar las unidades.');
        this.cdr.detectChanges();
      },
    });
  }
}
