import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

type Paso = 'correo' | 'codigo';

@Component({
  selector: 'app-restablecer-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './restablecer-password.component.html',
})
export class RestablecerPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  paso: Paso = 'correo';
  cargando = false;
  error = '';
  exito = '';
  correoEnviado = '';
  mostrarNueva = false;
  mostrarConfirmar = false;
  currentYear = new Date().getFullYear();

  formCorreo = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
  });

  formCodigo = this.fb.group(
    {
      codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      nuevaPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', Validators.required],
    },
    { validators: this.passwordsMatch }
  );

  private passwordsMatch(group: import('@angular/forms').AbstractControl) {
    const pass = group.get('nuevaPassword')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoinciden: true };
  }

  enviarCodigo(): void {
    if (this.formCorreo.invalid) {
      this.formCorreo.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.error = '';
    const correo = this.formCorreo.value.correo!;

    this.authService.solicitarCodigo(correo).subscribe({
      next: () => {
        this.correoEnviado = correo;
        this.paso = 'codigo';
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err.error?.mensaje ?? 'No se pudo enviar el código. Verifica el correo.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  resetPassword(): void {
    if (this.formCodigo.invalid) {
      this.formCodigo.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.error = '';

    const { codigo, nuevaPassword } = this.formCodigo.value;
    this.authService
      .resetPassword({ correo: this.correoEnviado, codigo: codigo!, nuevaPassword: nuevaPassword! })
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: (err) => {
          this.error = err.error?.mensaje ?? 'Código inválido o expirado.';
          this.cargando = false;
          this.cdr.detectChanges();
        },
      });
  }
}
