import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthAdminService } from '../../core/services/auth-admin.service';
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
    private authService: AuthAdminService,
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
        if (resp.rol !== 'ADMIN_SISTEMAS') {
          this.toast.advertencia(
            'Tu cuenta no tiene permisos para esta consola. Usa el front de revisión de quejas correspondiente a tu rol.',
          );
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
