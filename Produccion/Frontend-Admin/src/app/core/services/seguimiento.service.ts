import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  DetalleQuejaSeg,
  FiltroQuejasSeg,
  FiltrosSeg,
  PaginaQuejasSeg,
  TableroSeg,
  UnidadSeg,
} from '../models/admin.models';

/** Seguimiento de quejas de la Defensora (admin-service /api/admin/seguimiento). Solo lectura. */
@Injectable({ providedIn: 'root' })
export class SeguimientoService {
  private readonly apiUrl = '/api/admin/seguimiento';

  constructor(private http: HttpClient) {}

  tablero(): Observable<TableroSeg> {
    return this.http.get<TableroSeg>(`${this.apiUrl}/tablero`);
  }

  filtros(): Observable<FiltrosSeg> {
    return this.http.get<FiltrosSeg>(`${this.apiUrl}/filtros`);
  }

  quejas(filtro: FiltroQuejasSeg, pagina: number, tamanio = 15): Observable<PaginaQuejasSeg> {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor) {
        params = params.set(clave, valor);
      }
    }
    return this.http.get<PaginaQuejasSeg>(`${this.apiUrl}/quejas`, { params });
  }

  detalle(folio: string): Observable<DetalleQuejaSeg> {
    return this.http.get<DetalleQuejaSeg>(`${this.apiUrl}/quejas/${encodeURIComponent(folio)}`);
  }

  evidencia(folio: string, id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/quejas/${encodeURIComponent(folio)}/evidencias/${id}`, { responseType: 'blob' });
  }

  unidades(texto = ''): Observable<UnidadSeg[]> {
    const params = texto.trim() ? new HttpParams().set('q', texto.trim()) : undefined;
    return this.http.get<UnidadSeg[]>(`${this.apiUrl}/unidades`, { params });
  }
}
