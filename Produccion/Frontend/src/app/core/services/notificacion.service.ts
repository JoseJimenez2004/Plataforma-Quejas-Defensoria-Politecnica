import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

import { Notificacion } from '../models/notificacion.models';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly apiUrl = '/api/notificaciones';

  /** Contador compartido de no leídas — tanto la campanita del topbar (panel-layout) como
   * la página de Notificaciones leen/actualizan este mismo signal, así no se desincronizan
   * entre sí sin necesitar @Input/@Output entre componentes que no son padre-hijo directo. */
  readonly noLeidas = signal(0);

  constructor(private http: HttpClient) {}

  misNotificaciones(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.apiUrl}/mias`);
  }

  actualizarContador(): void {
    this.contarNoLeidas().subscribe({
      next: (n) => this.noLeidas.set(n),
      error: () => {},
    });
  }

  contarNoLeidas(): Observable<number> {
    // El backend regresa { "noLeidas": N }, no un number plano -- si no se extrae aquí, el
    // signal `noLeidas` terminaba guardando el objeto completo (siempre "truthy", por eso la
    // campanita del topbar mostraba el punto rojo aunque no hubiera nada sin leer).
    return this.http
      .get<{ noLeidas: number }>(`${this.apiUrl}/mias/no-leidas`)
      .pipe(map((resp) => resp.noLeidas));
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${this.apiUrl}/${id}/leida`, {}).pipe(
      tap(() => this.noLeidas.update((n) => Math.max(0, n - 1))),
    );
  }
}
