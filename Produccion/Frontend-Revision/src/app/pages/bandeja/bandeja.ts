import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { AuthRevisionService } from '../../core/services/auth-revision.service';
import { ToastService } from '../../core/services/toast.service';
import { BandejaResumen, QuejaResumenBandeja } from '../../core/models/revision.models';

/** Cada cuánto se vuelve a pedir la bandeja mientras la pantalla sigue abierta -- así, si
 * varios recepcionistas están viendo la lista al mismo tiempo, se van viendo entre ellos
 * (quién abrió qué queja) sin que nadie tenga que recargar la página a mano. */
const INTERVALO_REFRESCO_MS = 20000;

@Component({
  selector: 'app-bandeja',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.scss',
})
export class Bandeja implements OnInit, OnDestroy {
  resumen: BandejaResumen | null = null;
  cargando = true;

  private intervaloId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private revisionService: RevisionService,
    private authService: AuthRevisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.intervaloId = setInterval(() => this.cargar(/* silencioso */ true), INTERVALO_REFRESCO_MS);
  }

  ngOnDestroy(): void {
    if (this.intervaloId !== null) {
      clearInterval(this.intervaloId);
    }
  }

  /** true si la propia sesión es quien tiene abierta esta queja (puede continuar). */
  esMiRevision(item: QuejaResumenBandeja): boolean {
    const miNombre = this.authService.usuarioActual()?.nombre;
    return !!miNombre && item.revisandoPorNombre === miNombre;
  }

  /** true si OTRO recepcionista la tiene abierta ahora mismo -- se deshabilita para que no la
   * dupliquen; se libera sola si esa persona la deja inactiva (ver revision-service). */
  bloqueadaPorOtro(item: QuejaResumenBandeja): boolean {
    return item.estatus === 'EN_VALIDACION' && !!item.revisandoPorNombre && !this.esMiRevision(item);
  }

  private cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.revisionService.bandeja().subscribe({
      next: (resumen) => {
        this.resumen = resumen;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        if (!silencioso) {
          this.toast.error('No se pudo cargar la bandeja de entrada.');
        }
        this.cdr.detectChanges();
      },
    });
  }
}
