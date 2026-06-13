import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { NotificacionesService } from '../../../core/services/notificaciones.service';
import { NotificacionDTO } from '../../../models/notificacion.models';

type TabNotif = 'todas' | 'pendientes' | 'leidas';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './notificaciones.component.html',
})
export class NotificacionesComponent implements OnInit {
  private notifService = inject(NotificacionesService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  notificaciones: NotificacionDTO[] = [];
  tabActiva: TabNotif = 'todas';
  cargando = true;

  ngOnInit(): void {
    this.notifService.listar().subscribe({
      next: (n) => {
        this.notificaciones = n;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  get notifFiltradas(): NotificacionDTO[] {
    switch (this.tabActiva) {
      case 'pendientes': return this.notificaciones.filter((n) => !n.leida);
      case 'leidas': return this.notificaciones.filter((n) => n.leida);
      default: return this.notificaciones;
    }
  }

  get requerimientos(): NotificacionDTO[] {
    return this.notificaciones.filter((n) => n.tipo === 'REQUERIMIENTO' && !n.leida);
  }

  get cantidadPendientes(): number {
    return this.notificaciones.filter((n) => !n.leida).length;
  }

  marcarLeida(id: number): void {
    this.notifService.marcarLeida(id).subscribe({
      next: () => {
        const notif = this.notificaciones.find((n) => n.id === id);
        if (notif) notif.leida = true;
        this.cdr.detectChanges();
      },
    });
  }

  verConciliacion(folio: string): void {
    this.router.navigate(['/conciliacion', folio]);
  }
}
