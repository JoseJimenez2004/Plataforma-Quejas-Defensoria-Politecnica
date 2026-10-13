import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

/** Notificación del centro de notificaciones (notificaciones-service). */
interface NotificacionPersonal {
  id: number;
  tipo: string;
  titulo: string;
  mensaje: string;
  leida: boolean;
  fechaCreacion: string;
}

/**
 * Campana de notificaciones del PERSONAL (CU-NOT-01): cuenta las no leídas, lista las propias
 * y las marca como leídas. Usa el mismo JWT del panel (el interceptor lo agrega), y
 * notificaciones-service filtra por el correo del token.
 *
 * IMPORTANTE: este componente existe igual en Frontend-Revision y Frontend-Admin
 * (shared/campana-notificaciones/). Si cambias uno, cambia el otro.
 */
@Component({
  selector: 'app-campana-notificaciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './campana-notificaciones.html',
  styleUrl: './campana-notificaciones.scss',
})
export class CampanaNotificaciones implements OnInit, OnDestroy {
  private readonly apiUrl = '/api/notificaciones';
  private readonly intervaloMs = 60_000;
  private temporizador: ReturnType<typeof setInterval> | null = null;

  noLeidas = 0;
  abierta = false;
  cargando = false;
  disponible = true;
  notificaciones: NotificacionPersonal[] = [];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.contar();
    this.temporizador = setInterval(() => this.contar(), this.intervaloMs);
  }

  ngOnDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
  }

  @HostListener('document:click')
  cerrar(): void {
    if (this.abierta) {
      this.abierta = false;
      this.cdr.detectChanges();
    }
  }

  alternar(evento: Event): void {
    evento.stopPropagation();
    this.abierta = !this.abierta;
    if (this.abierta) {
      this.cargarLista();
    }
    this.cdr.detectChanges();
  }

  marcarLeida(n: NotificacionPersonal, evento: Event): void {
    evento.stopPropagation();
    if (n.leida) {
      return;
    }
    this.http.put<NotificacionPersonal>(`${this.apiUrl}/${n.id}/leida`, {}).subscribe({
      next: () => {
        n.leida = true;
        this.noLeidas = Math.max(0, this.noLeidas - 1);
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  marcarTodas(evento: Event): void {
    evento.stopPropagation();
    this.notificaciones.filter((n) => !n.leida).forEach((n) => this.marcarLeida(n, evento));
  }

  private contar(): void {
    this.http.get<{ noLeidas: number }>(`${this.apiUrl}/mias/no-leidas`).subscribe({
      next: (r) => {
        this.disponible = true;
        this.noLeidas = r?.noLeidas ?? 0;
        this.cdr.detectChanges();
      },
      // Si notificaciones-service no responde, la campana simplemente no muestra conteo.
      error: () => {
        this.disponible = false;
        this.cdr.detectChanges();
      },
    });
  }

  private cargarLista(): void {
    this.cargando = true;
    this.http.get<NotificacionPersonal[]>(`${this.apiUrl}/mias`).subscribe({
      next: (lista) => {
        this.notificaciones = (lista ?? []).slice(0, 30);
        this.noLeidas = this.notificaciones.filter((n) => !n.leida).length;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.disponible = false;
        this.cdr.detectChanges();
      },
    });
  }
}
