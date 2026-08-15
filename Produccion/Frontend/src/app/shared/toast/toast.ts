import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ToastService } from '../../core/services/toast.service';

/**
 * Contenedor global de notificaciones emergentes. Se monta una sola vez en `app.html` (fuera
 * del `<router-outlet>`), así que sobrevive a la navegación entre páginas y cualquier
 * componente puede disparar un aviso inyectando `ToastService`.
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-contenedor" aria-live="polite">
      @for (m of toastService.mensajes(); track m.id) {
        <div class="toast" [class]="'toast--' + m.tipo">
          <span class="toast__icono">{{ icono(m.tipo) }}</span>
          <span class="toast__texto">{{ m.texto }}</span>
          <button
            type="button"
            class="toast__cerrar"
            (click)="toastService.descartar(m.id)"
            aria-label="Cerrar aviso"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './toast.scss',
})
export class Toast {
  protected toastService = inject(ToastService);

  icono(tipo: string): string {
    switch (tipo) {
      case 'exito':
        return '✓';
      case 'error':
      case 'advertencia':
        return '⚠';
      default:
        return 'ℹ';
    }
  }
}
