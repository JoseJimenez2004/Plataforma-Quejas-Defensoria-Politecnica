import { Component } from '@angular/core';

import { SesionPersonalService } from '../../../core/services/sesion-personal.service';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class Header {

  constructor(
    private sesion: SesionPersonalService
  ) {}

  get nombreUsuario(): string {
    return this.sesion.getNombre() ?? 'Subdefensoría';
  }

  /** Iniciales para el avatar ("María López" → "ML"). */
  get iniciales(): string {
    const partes = this.nombreUsuario.split(/\s+/).filter((p) => p && !p.endsWith('.'));
    return ((partes[0]?.charAt(0) ?? '') + (partes[1]?.charAt(0) ?? '')).toUpperCase() || '?';
  }

  cerrarSesion(): void {
    this.sesion.cerrarSesion();

    window.location.assign('/revision/login');
  }
}