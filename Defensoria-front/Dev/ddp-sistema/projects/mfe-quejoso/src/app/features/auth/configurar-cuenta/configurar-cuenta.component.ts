import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../../../components/header/header.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-configurar-cuenta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule, HeaderComponent, FooterComponent],
  templateUrl: './configurar-cuenta.component.html',
})
export class ConfigurarCuentaComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  correo = '';
  folio = '';
  mostrarPassword = false;
  mostrarConfirmar = false;
  cargando = false;
  error = '';
  currentYear = new Date().getFullYear();

  form = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', Validators.required],
    },
    { validators: this.passwordsMatch }
  );

  private passwordsMatch(group: import('@angular/forms').AbstractControl) {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoinciden: true };
  }

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state ?? history.state;
    this.correo = state?.['correo'] ?? '';
    this.folio = state?.['folio'] ?? '';

    if (!this.correo || !this.folio) {
      this.router.navigate(['/']);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.error = '';

    const { password, confirmarPassword } = this.form.value;
    this.authService
      .activarCuenta({
        correo: this.correo,
        numeroFolio: this.folio,
        password: password!,
        confirmarPassword: confirmarPassword!,
      })
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: (err) => {
          this.error = err.error?.mensaje ?? 'Error al activar la cuenta.';
          this.cargando = false;
          this.cdr.detectChanges();
        },
      });
  }
}
