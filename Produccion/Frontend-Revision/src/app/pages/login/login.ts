import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthRevisionService } from '../../core/services/auth-revision.service';
import { ToastService } from '../../core/services/toast.service';
import {
  RUTA_PANEL_DEFENSORA,
  cerrarSesionPersonal,
  guardarSesionDefensora,
} from '../../core/sesion/sesion-personal';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  correo = '';
  password = '';
  mostrarPassword = false;
  cargando = false;
  error = '';
  errorCorreo = false;
  errorPassword = false;
  mayusculasActivas = false;

  constructor(
    private authService: AuthRevisionService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  limpiarError(): void {
    this.error = '';
    this.errorCorreo = false;
    this.errorPassword = false;
  }

  revisarMayusculas(evento: KeyboardEvent): void {
    this.mayusculasActivas = !!evento.getModifierState?.('CapsLock');
  }

  ingresar(): void {
    if (this.cargando) {
      return;
    }
    this.limpiarError();
    const correo = this.correo.trim();

    this.errorCorreo = !correo;
    this.errorPassword = !this.password;
    if (this.errorCorreo || this.errorPassword) {
      this.error = this.errorCorreo && this.errorPassword
        ? 'Escribe tu correo institucional y tu contraseña.'
        : this.errorCorreo
          ? 'Escribe tu correo institucional.'
          : 'Escribe tu contraseña.';
      return;
    }

    this.cargando = true;

    this.authService.login({ correo, password: this.password }).subscribe({
      next: (resp) => {
        this.cargando = false;

        switch (resp.rol) {

          case 'RECEPCIONISTA':
            // Se queda dentro del frontend de Revisión.
            this.router.navigate(['/']);
            break;

          case 'ANALISTA_PRIMER_CONTACTO':
            // Salta al frontend independiente de Primer Contacto.
            window.location.assign('/primer-contacto/');
            break;

          case 'SUBDEFENSOR':
            // Salta al frontend independiente de Subdefensoría.
            window.location.assign('/subdefensoria/');
            break;

          case 'DEFENSOR':
          case 'ADMIN_SISTEMAS':
            // Panel de la Defensora (Frontend-Admin): todo lo de administración y más.
            cerrarSesionPersonal();
            guardarSesionDefensora(resp);
            window.location.assign(RUTA_PANEL_DEFENSORA);
            break;

          default:
            this.toast.advertencia(
              'No existe una pantalla configurada para este rol.'
            );
            break;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        // Sin respuesta del servidor (status 0) o 5xx: no es culpa de las credenciales.
        if (!err?.status || err.status >= 500) {
          this.error = 'No pudimos conectar con el servidor. Intenta de nuevo en unos minutos.';
        } else {
          this.error = err?.error?.mensaje ?? 'Correo o contraseña incorrectos.';
          this.errorPassword = true;
        }
        this.cdr.detectChanges();
      },
    });
  }
}
