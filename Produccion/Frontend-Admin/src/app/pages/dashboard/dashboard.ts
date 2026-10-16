import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthAdminService } from '../../core/services/auth-admin.service';
import { AbogadoSeg, MesSeg, TableroSeg, UnidadSeg, etiquetaRol } from '../../core/models/admin.models';

/**
 * Panel general de la Defensora (2026-10-16): dónde están las quejas, cuántas llegan por mes,
 * de qué unidades, qué hace Recepción, cuánta carga lleva cada abogado y por qué se rechazan.
 * Cada número lleva al explorador de quejas ya filtrado.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  readonly etiquetaRol = etiquetaRol;
  tablero: TableroSeg | null = null;
  cargando = true;

  readonly fechaHoy = (() => {
    const t = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date());
    return t.charAt(0).toUpperCase() + t.slice(1);
  })();

  constructor(
    private seguimiento: SeguimientoService,
    private auth: AuthAdminService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.seguimiento.tablero().subscribe({
      next: (t) => {
        this.tablero = t;
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

  get titulo(): string {
    return this.auth.usuarioActual()?.rol === 'DEFENSOR' ? 'Panel de la Defensora' : 'Panel general';
  }

  /** Porcentaje entero (para anchos de barras). */
  pct(parte: number, total: number): number {
    return total > 0 ? Math.max(parte > 0 ? 2 : 0, Math.round((parte / total) * 100)) : 0;
  }

  get maxMes(): number {
    return Math.max(1, ...(this.tablero?.porMes ?? []).flatMap((m: MesSeg) => [m.recibidas, m.atendidas]));
  }

  get maxUnidad(): number {
    return Math.max(1, ...(this.tablero?.unidades ?? []).map((u: UnidadSeg) => u.total));
  }

  get maxAbogado(): number {
    return Math.max(1, ...(this.tablero?.abogados ?? []).map((a: AbogadoSeg) => a.enTramite));
  }

  get maxMotivo(): number {
    return Math.max(1, ...(this.tablero?.motivosRechazo ?? []).map((m) => m.total));
  }

  get totalEtapas(): number {
    return (this.tablero?.etapas ?? []).reduce((s, e) => s + e.total, 0);
  }

  nombreUnidad(u: UnidadSeg): string {
    return u.abreviatura || u.nombre || u.clave;
  }

  duracion(horas?: number | null): string {
    if (horas === null || horas === undefined) {
      return '—';
    }
    if (horas < 1) {
      return `${Math.max(1, Math.round(horas * 60))} min`;
    }
    return horas < 48 ? `${Math.round(horas)} h` : `${(horas / 24).toFixed(1)} días`;
  }

  /** Primer motivo, corto, para la lista de rechazos recientes. */
  motivoCorto(motivo?: string | null): string {
    const primero = (motivo ?? '').split(';')[0].trim();
    return primero.length > 110 ? primero.slice(0, 107) + '…' : primero;
  }
}
