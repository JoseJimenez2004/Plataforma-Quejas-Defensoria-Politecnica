import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Cambio de contraseña propia: mismo endpoint que Recepción y Frontend-Admin (admin-service lo
 * expone para CUALQUIER rol autenticado). El interceptor le agrega el JWT.
 */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly apiUrl = `${environment.adminApiUrl}/perfil`;

  constructor(private http: HttpClient) {}

  cambiarPassword(datos: { passwordActual: string; passwordNueva: string }): Observable<{ mensaje: string }> {
    return this.http.put<{ mensaje: string }>(`${this.apiUrl}/password`, datos);
  }
}
