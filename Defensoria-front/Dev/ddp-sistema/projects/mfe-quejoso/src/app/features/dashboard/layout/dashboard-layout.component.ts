import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionesService } from '../../../core/services/notificaciones.service';
import { UsuarioPerfilDTO } from '../../../models/auth.models';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './dashboard-layout.component.html',
})
export class DashboardLayoutComponent implements OnInit {
  private authService = inject(AuthService);
  private notifService = inject(NotificacionesService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  perfil: UsuarioPerfilDTO | null = null;
  notifNoLeidas = 0;
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.perfil = this.authService.getPerfil();
    this.cargarNotificaciones();
  }

  private cargarNotificaciones(): void {
    this.notifService.listar().subscribe({
      next: (notifs) => {
        this.notifNoLeidas = notifs.filter((n) => !n.leida).length;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  cerrarSesion(): void {
    this.authService.logout();
  }
}
