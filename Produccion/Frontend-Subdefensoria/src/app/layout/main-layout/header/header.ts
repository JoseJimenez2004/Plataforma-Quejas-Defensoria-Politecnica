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

  cerrarSesion(): void {
    this.sesion.cerrarSesion();

    window.location.assign('/revision/login');
  }
}