import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  mensajeError: string = '';
  
  // Nueva variable para controlar el "ojito"
  mostrarPassword = false;

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

  // Función que alterna entre ver y ocultar
  togglePassword() {
    this.mostrarPassword = !this.mostrarPassword;
  }

  iniciarSesion() {
    if (this.loginForm.valid) {
      const { correo, password } = this.loginForm.value;
      
      this.authService.login(correo, password).subscribe({
        next: (res) => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.mensajeError = 'Credenciales incorrectas o acceso denegado.';
        }
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }

  irARecuperar() {
    this.router.navigate(['/recuperar-password']);
  }
}