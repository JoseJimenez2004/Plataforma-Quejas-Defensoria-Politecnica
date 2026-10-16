import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ChatbotWidget } from '../chatbot-widget/chatbot-widget';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ChatbotWidget],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  readonly anio = new Date().getFullYear();

  private readonly auth = inject(AuthService);

  /** Con sesión iniciada (p. ej. al abrir una cita desde las notificaciones) el encabezado
   *  lleva al panel en vez de pedir iniciar sesión otra vez. */
  get haySesion(): boolean {
    return this.auth.isLoggedIn();
  }
}
