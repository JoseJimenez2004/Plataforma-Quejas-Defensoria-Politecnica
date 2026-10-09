import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthRevisionService } from '../../core/services/auth-revision.service';
import { ToastService } from '../../core/services/toast.service';

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

  constructor(
    private authService: AuthRevisionService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ingresar(): void {
    this.error = '';
    this.cargando = true;

    this.authService.login({ correo: this.correo, password: this.password }).subscribe({
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

          case 'ADMIN_SISTEMAS':
            this.toast.advertencia(
              'Tu cuenta corresponde a Administración. Utiliza la consola administrativa.'
            );
            break;

          case 'DEFENSOR':
            this.toast.advertencia(
              'La vista correspondiente al rol Defensor todavía no está disponible.'
            );
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
        this.error = err?.error?.mensaje ?? 'Credenciales incorrectas.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });
  }
}
