import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  mostrarPassword = false;
  cargando = false;
  error = '';
  currentYear = new Date().getFullYear();

  togglePassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
  }

  submit(): void {
    if (this.form.invalid) return;
    this.cargando = true;
    this.error = '';

    const { correo, password } = this.form.value;
    this.authService.login({ correo: correo!, password: password! }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.error?.mensaje ?? 'Credenciales incorrectas. Intenta de nuevo.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
