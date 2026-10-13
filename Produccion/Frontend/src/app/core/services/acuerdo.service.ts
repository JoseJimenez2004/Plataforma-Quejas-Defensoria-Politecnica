import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AcuerdoPublico } from '../models/acuerdo.models';

/** Cliente de subdefensoria-service para que el quejoso responda el acuerdo de conciliación
 * desde el enlace del correo o de su notificación (CU-QJ-14). Ruta pública: el token del
 * acuerdo es la credencial, no hace falta sesión. */
@Injectable({ providedIn: 'root' })
export class AcuerdoService {
  private readonly apiUrl = '/api/subdefensoria/publico/acuerdos';

  constructor(private http: HttpClient) {}

  consultar(token: string): Observable<AcuerdoPublico> {
    return this.http.get<AcuerdoPublico>(`${this.apiUrl}/${encodeURIComponent(token)}`);
  }

  aceptar(token: string, comentario: string): Observable<AcuerdoPublico> {
    return this.http.put<AcuerdoPublico>(`${this.apiUrl}/${encodeURIComponent(token)}/aceptar`, {
      comentario: comentario || null,
    });
  }

  rechazar(token: string, comentario: string): Observable<AcuerdoPublico> {
    return this.http.put<AcuerdoPublico>(`${this.apiUrl}/${encodeURIComponent(token)}/rechazar`, {
      comentario,
    });
  }
}
