import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil {
  correoPersonal = '';
  telefonoCelular = '';
  guardado = false;

  constructor(public authService: AuthService) {}

  guardarCambios(): void {
    // TODO(backend): no existe un endpoint para actualizar correo personal / teléfono /
    // datos de tutor del usuario logueado. Ver docs/HALLAZGOS.md.
    this.guardado = true;
    setTimeout(() => (this.guardado = false), 2500);
  }
}
