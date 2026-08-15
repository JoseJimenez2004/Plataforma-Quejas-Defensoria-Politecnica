import { Injectable, signal } from '@angular/core';

export interface ToastMensaje {
  id: number;
  texto: string;
  tipo: 'exito' | 'error' | 'advertencia' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private contador = 0;
  readonly mensajes = signal<ToastMensaje[]>([]);

  exito(texto: string): void {
    this.agregar(texto, 'exito');
  }

  error(texto: string): void {
    this.agregar(texto, 'error');
  }

  advertencia(texto: string): void {
    this.agregar(texto, 'advertencia');
  }

  info(texto: string): void {
    this.agregar(texto, 'info');
  }

  descartar(id: number): void {
    this.mensajes.update((lista) => lista.filter((m) => m.id !== id));
  }

  private agregar(texto: string, tipo: ToastMensaje['tipo']): void {
    const id = ++this.contador;
    this.mensajes.update((lista) => [...lista, { id, texto, tipo }]);
    setTimeout(() => this.descartar(id), 4500);
  }
}
