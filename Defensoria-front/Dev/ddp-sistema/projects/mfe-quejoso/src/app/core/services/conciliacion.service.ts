import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AcuerdoPendienteDTO, ConciliacionDTO } from '../../models/perfil.models';

const BASE = 'http://localhost:8080/api/quejoso/conciliacion';

@Injectable({ providedIn: 'root' })
export class ConciliacionService {
  private http = inject(HttpClient);

  listarPendientes(): Observable<AcuerdoPendienteDTO[]> {
    return this.http.get<AcuerdoPendienteDTO[]>(`${BASE}/pendientes`);
  }

  obtener(folio: string): Observable<ConciliacionDTO> {
    return this.http.get<ConciliacionDTO>(`${BASE}/${folio}`);
  }

  aceptar(folio: string): Observable<string> {
    return this.http.post(`${BASE}/${folio}/aceptar`, null, { responseType: 'text' });
  }

  rechazar(folio: string, justificacion: string): Observable<string> {
    return this.http.post(`${BASE}/${folio}/rechazar`, { justificacion }, { responseType: 'text' });
  }
}
