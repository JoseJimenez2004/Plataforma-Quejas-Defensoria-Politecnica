import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CitaPublica } from '../models/cita.models';

/** Cliente de primer-contacto-service para que el quejoso responda su cita desde el enlace
 * del correo o de su notificación. Ruta pública: el token de la cita es la credencial, no
 * hace falta sesión. */
@Injectable({ providedIn: 'root' })
export class CitaService {
  private readonly apiUrl = '/api/primer-contacto/publico/citas';

  constructor(private http: HttpClient) {}

  consultar(token: string): Observable<CitaPublica> {
    return this.http.get<CitaPublica>(`${this.apiUrl}/${encodeURIComponent(token)}`);
  }

  confirmar(token: string): Observable<CitaPublica> {
    return this.http.put<CitaPublica>(`${this.apiUrl}/${encodeURIComponent(token)}/confirmar`, {});
  }

  cancelar(token: string, motivo: string): Observable<CitaPublica> {
    return this.http.put<CitaPublica>(`${this.apiUrl}/${encodeURIComponent(token)}/cancelar`, {
      motivo,
    });
  }
}
