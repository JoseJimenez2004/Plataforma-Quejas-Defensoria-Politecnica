import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './recuperar-password.html',
  styleUrl: './recuperar-password.scss',
})
export class RecuperarPassword {
  paso: 1 | 2 = 1;

  correo = '';
  codigo = '';
  nuevaPassword = '';
  confirmarPassword = '';

  cargando = false;
  error = '';
  mensaje = '';

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  solicitarCodigo(): void {
    this.error = '';
    this.cargando = true;
    this.authService.solicitarCodigo(this.correo).subscribe({
      next: (msg) => {
        this.cargando = false;
        this.mensaje = msg;
        this.paso = 2;
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudo enviar el código. Verifica el correo.';
      },
    });
  }

  restablecer(): void {
    this.error = '';

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.cargando = true;
    this.authService
      .resetPassword({
        correo: this.correo,
        codigo: this.codigo,
        nuevaPassword: this.nuevaPassword,
      })
      .subscribe({
        next: () => {
          this.cargando = false;
          this.router.navigate(['/portal/login']);
        },
        error: (err) => {
          this.cargando = false;
          this.error = err?.error?.mensaje ?? 'El código es incorrecto o expiró.';
        },
      });
  }
}
