import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-panel-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './panel-layout.html',
  styleUrl: './panel-layout.scss',
})
export class PanelLayout implements OnInit {
  constructor(
    public authService: AuthService,
    public notificacionService: NotificacionService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.notificacionService.actualizarContador();
  }

  get iniciales(): string {
    const nombre = this.authService.usuarioActual()?.nombre ?? '';
    const partes = nombre.trim().split(/\s+/).filter(Boolean);
    if (!partes.length) return '?';
    return (partes[0][0] + (partes[1]?.[0] ?? '')).toUpperCase();
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/portal/login']);
  }
}
