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
        if (resp.rol === 'ADMIN_SISTEMAS') {
          this.toast.advertencia('Tu cuenta es de Admin. de Sistemas -- usa la consola de administración, no este panel.');
        } else if (resp.rol !== 'RECEPCIONISTA') {
          this.toast.advertencia('Las vistas para tu rol todavía no están disponibles en este panel.');
        }
        this.router.navigate(['/']);
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
