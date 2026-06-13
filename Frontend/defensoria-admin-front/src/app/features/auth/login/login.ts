import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth'; // <-- Ojo a esta ruta basada en tu estructura
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html', // <-- Apunta a tu archivo HTML
  styleUrls: ['./login.css'] // <-- Apunta a tu archivo CSS
})
export class LoginComponent {
  loginForm: FormGroup;
  mensajeError: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      correo: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  iniciarSesion() {
    if (this.loginForm.valid) {
      const { correo, password } = this.loginForm.value;
      
      this.authService.login(correo, password).subscribe({
        next: (res) => {
          console.log('¡Bienvenido!', res);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          console.error(err);
          this.mensajeError = 'Credenciales incorrectas o acceso denegado.';
        }
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }
}