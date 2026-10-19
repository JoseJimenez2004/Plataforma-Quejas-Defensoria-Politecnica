import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CargaTrabajo,
  DetallePersonal,
  DetalleQuejaSeg,
  FiltroCasos,
  FiltroQuejasSeg,
  FiltroUnidades,
  FiltrosSeg,
  MiembroPersonal,
  MotivoRechazo,
  PaginaCasos,
  PaginaQuejasSeg,
  PanelDefensora,
  TablaUnidades,
  UnidadSeg,
} from '../models/admin.models';

/** Panel de la Defensora (admin-service /api/admin/seguimiento). */
@Injectable({ providedIn: 'root' })
export class SeguimientoService {
  private readonly apiUrl = '/api/admin/seguimiento';

  constructor(private http: HttpClient) {}

  tablero(dias = 30, meses = 12): Observable<PanelDefensora> {
    return this.http.get<PanelDefensora>(`${this.apiUrl}/tablero`, { params: { dias, meses } });
  }

  atencion(filtro: FiltroCasos, pagina = 0, tamanio = 10): Observable<PaginaCasos> {
    return this.http.get<PaginaCasos>(`${this.apiUrl}/atencion`, { params: parametros(filtro, pagina, tamanio) });
  }

  filtros(): Observable<FiltrosSeg> {
    return this.http.get<FiltrosSeg>(`${this.apiUrl}/filtros`);
  }

  quejas(filtro: FiltroQuejasSeg, pagina: number, tamanio = 10): Observable<PaginaQuejasSeg> {
    return this.http.get<PaginaQuejasSeg>(`${this.apiUrl}/quejas`, { params: parametros(filtro, pagina, tamanio) });
  }

  detalle(folio: string): Observable<DetalleQuejaSeg> {
    return this.http.get<DetalleQuejaSeg>(`${this.apiUrl}/quejas/${encodeURIComponent(folio)}`);
  }

  evidencia(folio: string, id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/quejas/${encodeURIComponent(folio)}/evidencias/${id}`, { responseType: 'blob' });
  }

  /** Lista simple (para selectores). */
  unidades(texto = ''): Observable<UnidadSeg[]> {
    const params = texto.trim() ? new HttpParams().set('q', texto.trim()) : undefined;
    return this.http.get<UnidadSeg[]>(`${this.apiUrl}/unidades`, { params });
  }

  tablaUnidades(filtro: FiltroUnidades, dias: number, pagina = 0, tamanio = 10): Observable<TablaUnidades> {
    return this.http.get<TablaUnidades>(`${this.apiUrl}/unidades/tabla`, {
      params: parametros({ ...filtro, dias }, pagina, tamanio),
    });
  }

  carga(dias = 30): Observable<CargaTrabajo> {
    return this.http.get<CargaTrabajo>(`${this.apiUrl}/carga`, { params: { dias } });
  }

  motivos(filtro: { q?: string; categoria?: string; desde?: string; hasta?: string }, dias = 0): Observable<MotivoRechazo[]> {
    return this.http.get<MotivoRechazo[]>(`${this.apiUrl}/motivos-rechazo`, { params: parametros({ ...filtro, dias }) });
  }

  personal(): Observable<MiembroPersonal[]> {
    return this.http.get<MiembroPersonal[]>(`${this.apiUrl}/personal`);
  }

  detallePersonal(id: number): Observable<DetallePersonal> {
    return this.http.get<DetallePersonal>(`${this.apiUrl}/personal/${id}`);
  }

  guardarContacto(id: number, datos: { unidadClave?: string | null; telefono?: string | null }): Observable<MiembroPersonal> {
    return this.http.put<MiembroPersonal>(`${this.apiUrl}/personal/${id}/contacto`, datos);
  }
}

/** Convierte un filtro en HttpParams sin mandar los vacíos. */
function parametros(filtro: object, pagina?: number, tamanio?: number): HttpParams {
  let params = new HttpParams();
  if (pagina !== undefined) {
    params = params.set('pagina', pagina);
  }
  if (tamanio !== undefined) {
    params = params.set('tamanio', tamanio);
  }
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      params = params.set(clave, String(valor));
    }
  }
  return params;
}
