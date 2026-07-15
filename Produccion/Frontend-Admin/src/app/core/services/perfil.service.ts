import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CambiarPasswordRequest } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly apiUrl = '/api/admin/perfil';

  constructor(private http: HttpClient) {}

  cambiarPassword(datos: CambiarPasswordRequest): Observable<{ mensaje: string }> {
    return this.http.put<{ mensaje: string }>(`${this.apiUrl}/password`, datos);
  }
}
