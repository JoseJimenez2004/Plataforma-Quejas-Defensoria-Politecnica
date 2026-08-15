import { Injectable, signal } from '@angular/core';

export type ToastTipo = 'exito' | 'error' | 'advertencia' | 'info';

export interface ToastMensaje {
  id: number;
  tipo: ToastTipo;
  texto: string;
}

/**
 * Servicio global de notificaciones emergentes (toasts). Antes cada formulario tenía a lo
 * mucho un párrafo rojo estático al fondo (`.error`), que el usuario podía no ver si ya
 * había hecho scroll. Este servicio permite disparar avisos flotantes desde cualquier
 * componente, visibles de inmediato sin importar el scroll, y que se autodescartan solos.
 *
 * Uso: inyectar `ToastService` y llamar `.exito(...)`, `.error(...)`, `.advertencia(...)` o
 * `.info(...)`. El componente `app-toast` (montado una sola vez en `app.html`) los renderiza.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private contador = 0;
  readonly mensajes = signal<ToastMensaje[]>([]);

  private mostrar(tipo: ToastTipo, texto: string, duracionMs: number): void {
    const id = ++this.contador;
    this.mensajes.update((actuales) => [...actuales, { id, tipo, texto }]);
    if (duracionMs > 0) {
      setTimeout(() => this.descartar(id), duracionMs);
    }
  }

  exito(texto: string, duracionMs = 5000): void {
    this.mostrar('exito', texto, duracionMs);
  }

  error(texto: string, duracionMs = 7000): void {
    this.mostrar('error', texto, duracionMs);
  }

  advertencia(texto: string, duracionMs = 6500): void {
    this.mostrar('advertencia', texto, duracionMs);
  }

  info(texto: string, duracionMs = 5000): void {
    this.mostrar('info', texto, duracionMs);
  }

  descartar(id: number): void {
    this.mensajes.update((actuales) => actuales.filter((m) => m.id !== id));
  }
}
