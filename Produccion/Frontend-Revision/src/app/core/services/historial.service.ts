import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { HistorialItem } from '../models/revision.models';

@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly apiUrl = '/api/revision/historial';

  constructor(private http: HttpClient) {}

  listar(texto?: string, estatus?: string, fechaDesde?: string, fechaHasta?: string): Observable<HistorialItem[]> {
    const params: Record<string, string> = {};
    if (texto) params['texto'] = texto;
    if (estatus) params['estatus'] = estatus;
    if (fechaDesde) params['fechaDesde'] = fechaDesde;
    if (fechaHasta) params['fechaHasta'] = fechaHasta;
    return this.http.get<HistorialItem[]>(this.apiUrl, { params });
  }

  /** El endpoint exige JWT -- se descarga como blob (no un <a href> directo) para poder
   * mandar el Authorization header, igual que la descarga de evidencias. */
  exportar(texto?: string, estatus?: string, fechaDesde?: string, fechaHasta?: string): Observable<Blob> {
    const params: Record<string, string> = {};
    if (texto) params['texto'] = texto;
    if (estatus) params['estatus'] = estatus;
    if (fechaDesde) params['fechaDesde'] = fechaDesde;
    if (fechaHasta) params['fechaHasta'] = fechaHasta;
    return this.http.get(`${this.apiUrl}/exportar`, { params, responseType: 'blob' });
  }
}
