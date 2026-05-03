import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-activar-folio',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './activar-folio.component.html',
})
export class ActivarFolioComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  form = this.fb.group(
    {
      correo: ['', [Validators.required, Validators.email]],
      numeroFolio: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', Validators.required],
    },
    { validators: this.passwordsMatch }
  );

  cargando = false;
  error = '';
  mostrarPassword = false;
  mostrarConfirmar = false;
  currentYear = new Date().getFullYear();

  private passwordsMatch(group: import('@angular/forms').AbstractControl) {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoinciden: true };
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.error = '';

    const { correo, numeroFolio, password, confirmarPassword } = this.form.value;
    this.authService
      .activarCuenta({
        correo: correo!,
        numeroFolio: numeroFolio!,
        password: password!,
        confirmarPassword: confirmarPassword!,
      })
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: (err) => {
          this.error = err.error?.mensaje ?? 'Error al activar la cuenta. Verifica los datos.';
          this.cargando = false;
          this.cdr.detectChanges();
        },
      });
  }
}
