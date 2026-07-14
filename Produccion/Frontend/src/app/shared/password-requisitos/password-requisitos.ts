import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/** Checklist animado de requisitos de contraseña (8 caracteres, mayúscula, número) — se usa
 * en cualquier pantalla donde el usuario defina una contraseña nueva (activar cuenta,
 * restablecer contraseña). Cada requisito se marca en verde con una animación en cuanto se
 * cumple, en vez de solo mostrar un texto de ayuda estático. */
@Component({
  selector: 'app-password-requisitos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './password-requisitos.html',
  styleUrl: './password-requisitos.scss',
})
export class PasswordRequisitos {
  @Input() password = '';

  get tieneLongitud(): boolean {
    return this.password.length >= 8;
  }

  get tieneMayuscula(): boolean {
    return /[A-Z]/.test(this.password);
  }

  get tieneNumero(): boolean {
    return /\d/.test(this.password);
  }

  get cumpleTodo(): boolean {
    return this.tieneLongitud && this.tieneMayuscula && this.tieneNumero;
  }
}
