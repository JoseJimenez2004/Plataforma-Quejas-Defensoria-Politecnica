import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioPerfilDTO } from '../../models/auth.models';

const BASE = 'http://localhost:8080/api/quejoso/perfil';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private http = inject(HttpClient);

  obtener(): Observable<UsuarioPerfilDTO> {
    return this.http.get<UsuarioPerfilDTO>(BASE);
  }

  actualizar(dto: UsuarioPerfilDTO): Observable<string> {
    return this.http.put(`${BASE}/actualizar`, dto, { responseType: 'text' });
  }
}
