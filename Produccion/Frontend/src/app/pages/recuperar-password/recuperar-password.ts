import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { PasswordRequisitos } from '../../shared/password-requisitos/password-requisitos';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PasswordRequisitos],
  templateUrl: './recuperar-password.html',
  styleUrl: './recuperar-password.scss',
})
export class RecuperarPassword {
  paso: 1 | 2 = 1;

  correo = '';
  codigo = '';
  nuevaPassword = '';
  confirmarPassword = '';
  mostrarPassword = false;
  mostrarConfirmar = false;

  cargando = false;
  error = '';
  mensaje = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  solicitarCodigo(): void {
    this.error = '';
    this.cargando = true;
    this.authService.solicitarCodigo(this.correo).subscribe({
      next: (resp) => {
        this.cargando = false;
        this.mensaje = resp.mensaje;
        this.paso = 2;
        this.toast.exito('Código de recuperación enviado a tu correo.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudo enviar el código. Verifica el correo.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });
  }

  restablecer(): void {
    this.error = '';

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      this.toast.error(this.error);
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
          this.toast.exito('Contraseña actualizada con éxito.');
          this.router.navigate(['/portal/login']);
        },
        error: (err) => {
          this.cargando = false;
          this.error = err?.error?.mensaje ?? 'El código es incorrecto o expiró.';
          this.toast.error(this.error);
          this.cdr.detectChanges();
        },
      });
  }
}
