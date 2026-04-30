import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { QuejaRegistroDTO, QuejaEditarDTO, QuejaSeguimientoDTO } from '../../models/queja.models';

const BASE = 'http://localhost:8080/api/quejoso/quejas';

@Injectable({ providedIn: 'root' })
export class QuejaService {
  private http = inject(HttpClient);

  registrar(dto: QuejaRegistroDTO, archivos: File[]): Observable<QuejaSeguimientoDTO> {
    const formData = new FormData();
    formData.append('datos', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    archivos.forEach((f) => formData.append('archivos', f));
    return this.http.post<QuejaSeguimientoDTO>(`${BASE}/registrar`, formData);
  }

  seguimientoPublico(folio: string, correo: string): Observable<QuejaSeguimientoDTO> {
    return this.http.get<QuejaSeguimientoDTO>(`${BASE}/seguimiento/publico`, {
      params: { folio, correo },
    });
  }

  seguimientoPrivado(folio: string): Observable<QuejaSeguimientoDTO> {
    return this.http.get<QuejaSeguimientoDTO>(`${BASE}/seguimiento/privado/${folio}`);
  }

  editar(folio: string, dto: QuejaEditarDTO, archivos: File[]): Observable<string> {
    const formData = new FormData();
    formData.append('datos', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    archivos.forEach((f) => formData.append('nuevosArchivos', f));
    return this.http.put(`${BASE}/editar/${folio}`, formData, { responseType: 'text' });
  }

  borrar(folio: string): Observable<string> {
    return this.http.delete(`${BASE}/borrar/${folio}`, { responseType: 'text' });
  }
}
