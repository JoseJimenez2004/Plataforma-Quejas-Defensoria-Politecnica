import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-activar-cuenta',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './activar-cuenta.html',
  styleUrl: './activar-cuenta.scss',
})
export class ActivarCuenta {
  correo = '';
  numeroFolio = '';
  password = '';
  confirmarPassword = '';

  cargando = false;
  error = '';
  exito = false;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  activar(): void {
    this.error = '';

    if (this.password !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.cargando = true;
    this.authService
      .activarCuenta({
        correo: this.correo,
        numeroFolio: this.numeroFolio,
        password: this.password,
        confirmarPassword: this.confirmarPassword,
      })
      .subscribe({
        next: () => {
          this.cargando = false;
          this.exito = true;
          setTimeout(() => this.router.navigate(['/portal/login']), 2500);
        },
        error: (err) => {
          this.cargando = false;
          // /activar-cuenta no tiene try/catch propio en el controlador — el error
          // pasa por GlobalExceptionHandler, que responde { mensaje, timestamp, codigo }.
          this.error = err?.error?.mensaje ?? 'No se pudo activar la cuenta. Verifica tus datos.';
        },
      });
  }
}
