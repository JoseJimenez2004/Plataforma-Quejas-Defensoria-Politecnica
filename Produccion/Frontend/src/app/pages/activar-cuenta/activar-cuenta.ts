import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { PasswordRequisitos } from '../../shared/password-requisitos/password-requisitos';

@Component({
  selector: 'app-activar-cuenta',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PasswordRequisitos],
  templateUrl: './activar-cuenta.html',
  styleUrl: './activar-cuenta.scss',
})
export class ActivarCuenta implements OnInit {
  correo = '';
  numeroFolio = '';
  password = '';
  confirmarPassword = '';
  mostrarPassword = false;
  mostrarConfirmar = false;

  cargando = false;
  error = '';
  exito = false;

  /** true cuando se llega desde la pantalla de éxito del registro (con ?correo=&folio= en la
   * URL) — en ese caso ya sabemos folio y correo, así que se ocultan esos campos y se muestra
   * el indicador de pasos "Registro → Folio → Activar Cuenta" en vez del formulario suelto. */
  modoPrefilled = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const correo = params.get('correo');
    const folio = params.get('folio');
    if (correo && folio) {
      this.correo = correo;
      this.numeroFolio = folio;
      this.modoPrefilled = true;
    }
  }

  activar(): void {
    this.error = '';

    if (this.password !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      this.toast.error(this.error);
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
          this.toast.exito('¡Cuenta activada con éxito!');
          // App zoneless (sin zone.js): sin esto, la vista no refleja "exito=true" y se
          // queda en el formulario aunque la cuenta ya se haya activado por dentro.
          this.cdr.detectChanges();
          setTimeout(() => this.router.navigate(['/portal/login']), 2500);
        },
        error: (err) => {
          this.cargando = false;
          // /activar-cuenta no tiene try/catch propio en el controlador — el error
          // pasa por GlobalExceptionHandler, que responde { mensaje, timestamp, codigo }.
          this.error = err?.error?.mensaje ?? 'No se pudo activar la cuenta. Verifica tus datos.';
          this.toast.error(this.error);
          this.cdr.detectChanges();
        },
      });
  }
}
