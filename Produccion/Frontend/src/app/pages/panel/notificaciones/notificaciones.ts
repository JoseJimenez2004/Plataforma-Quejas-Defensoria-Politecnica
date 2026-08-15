import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { NotificacionService } from '../../../core/services/notificacion.service';
import { Notificacion } from '../../../core/models/notificacion.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.scss',
})
export class Notificaciones implements OnInit {
  notificaciones: Notificacion[] = [];
  cargando = true;
  error = '';

  constructor(
    public notificacionService: NotificacionService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.notificacionService.misNotificaciones().subscribe({
      next: (notificaciones) => {
        this.notificaciones = notificaciones;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudieron cargar tus notificaciones.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });
  }

  icono(tipo: string): string {
    switch (tipo) {
      case 'LOGIN':
        return '🔐';
      case 'CAMBIO_ESTATUS':
        return '📋';
      case 'CONCILIACION':
        return '🤝';
      default:
        return '🔔';
    }
  }

  tiempoRelativo(fechaIso: string): string {
    if (!fechaIso) return '';
    const fecha = new Date(fechaIso);
    const diffMs = Date.now() - fecha.getTime();
    const minutos = Math.floor(diffMs / 60000);
    if (minutos < 1) return 'Justo ahora';
    if (minutos < 60) return `Hace ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `Hace ${horas} h`;
    const dias = Math.floor(horas / 24);
    if (dias === 1) return 'Ayer';
    if (dias < 7) return `Hace ${dias} días`;
    return fecha.toLocaleDateString('es-MX', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  abrir(n: Notificacion): void {
    if (!n.leida) {
      this.notificacionService.marcarLeida(n.id).subscribe({
        next: (actualizada) => {
          n.leida = actualizada.leida;
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    }
    if (n.enlace) {
      this.router.navigateByUrl(n.enlace);
    }
  }

  get hayNoLeidas(): boolean {
    return this.notificaciones.some((n) => !n.leida);
  }

  marcarTodasLeidas(): void {
    const pendientes = this.notificaciones.filter((n) => !n.leida);
    for (const n of pendientes) {
      this.notificacionService.marcarLeida(n.id).subscribe({
        next: (actualizada) => {
          n.leida = actualizada.leida;
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    }
  }
}
