import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Igual endpoint que usa Frontend-Admin (admin-service expone el cambio de contraseña
 * propia para CUALQUIER rol autenticado, no solo Admin de Sistemas). */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly apiUrl = '/api/admin/perfil';

  constructor(private http: HttpClient) {}

  cambiarPassword(datos: { passwordActual: string; passwordNueva: string }): Observable<{ mensaje: string }> {
    return this.http.put<{ mensaje: string }>(`${this.apiUrl}/password`, datos);
  }
}
