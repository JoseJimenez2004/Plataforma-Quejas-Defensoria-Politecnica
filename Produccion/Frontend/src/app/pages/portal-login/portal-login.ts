import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-portal-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './portal-login.html',
  styleUrl: './portal-login.scss',
})
export class PortalLogin {
  correo = '';
  password = '';
  mostrarPassword = false;
  cargando = false;
  error = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ingresar(): void {
    this.error = '';
    this.cargando = true;

    this.authService.login({ correo: this.correo, password: this.password }).subscribe({
      next: () => {
        this.cargando = false;
        // Bug original: aquí navegaba a '/dashboard', ruta que nunca existió.
        // Corregido: el panel autenticado vive en '/panel'.
        this.router.navigate(['/panel']);
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error ?? 'Credenciales incorrectas.';
        // App zoneless (sin zone.js): sin esto, el botón se queda pegado en "Ingresando…"
        // porque una respuesta HTTP asíncrona no dispara sola un refresco de la vista.
        this.cdr.detectChanges();
      },
    });
  }
}
