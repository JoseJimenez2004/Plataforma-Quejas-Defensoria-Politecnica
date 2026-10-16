import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AntecedenteItem,
  AreaOpcion,
  CargaAbogado,
  BandejaResumen,
  DefensorOpcion,
  FiltroBandeja,
  IdentificacionOficialOpcion,
  QuejaDetalle,
  RechazarQuejaRequest,
  RegistroManualResponse,
  TurnarQuejaRequest,
} from '../models/revision.models';

@Injectable({ providedIn: 'root' })
export class RevisionService {
  private readonly apiUrl = '/api/revision';

  constructor(private http: HttpClient) {}

  /** Una página de la cola (10 por defecto, la más reciente arriba) + indicadores + equipo. */
  bandeja(filtro: FiltroBandeja = 'todas', texto = '', pagina = 0, tamanio = 10): Observable<BandejaResumen> {
    let params = new HttpParams().set('filtro', filtro).set('pagina', pagina).set('tamanio', tamanio);
    if (texto.trim()) {
      params = params.set('q', texto.trim());
    }
    return this.http.get<BandejaResumen>(`${this.apiUrl}/bandeja`, { params });
  }

  /** Suelta la queja (la regresa a la cola para que la tome otra recepcionista). */
  liberar(folio: string): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/quejas/${folio}/liberar`, {});
  }

  /** Mantiene la queja a nombre de quien la revisa mientras sigue abierta en pantalla. */
  renovar(folio: string): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/quejas/${folio}/renovar`, {});
  }

  detalle(folio: string): Observable<QuejaDetalle> {
    return this.http.get<QuejaDetalle>(`${this.apiUrl}/quejas/${folio}`);
  }

  antecedentes(folio: string): Observable<AntecedenteItem[]> {
    return this.http.get<AntecedenteItem[]>(`${this.apiUrl}/quejas/${folio}/antecedentes`);
  }

  /** Abogados con su carga de trabajo y cuál sugiere el sistema para esta queja. */
  abogados(folio: string): Observable<CargaAbogado[]> {
    return this.http.get<CargaAbogado[]>(`${this.apiUrl}/quejas/${folio}/abogados`);
  }

  urlEvidencia(id: number): string {
    return `${this.apiUrl}/quejas/evidencias/${id}`;
  }

  /** El endpoint exige JWT, así que un <a href> normal no sirve (el navegador no manda el
   * Authorization header en una navegación directa) -- se descarga como blob y se abre. */
  descargarEvidencia(id: number): Observable<Blob> {
    return this.http.get(this.urlEvidencia(id), { responseType: 'blob' });
  }

  rechazar(folio: string, datos: RechazarQuejaRequest): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/quejas/${folio}/rechazar`, datos);
  }

  turnar(folio: string, datos: TurnarQuejaRequest): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/quejas/${folio}/turnar`, datos);
  }

  areas(): Observable<AreaOpcion[]> {
    return this.http.get<AreaOpcion[]>(`${this.apiUrl}/catalogos/areas`);
  }

  defensores(): Observable<DefensorOpcion[]> {
    return this.http.get<DefensorOpcion[]>(`${this.apiUrl}/catalogos/defensores`);
  }

  identificacionesOficiales(): Observable<IdentificacionOficialOpcion[]> {
    return this.http.get<IdentificacionOficialOpcion[]>(`${this.apiUrl}/catalogos/identificaciones-oficiales`);
  }

  registrarManual(formData: FormData): Observable<RegistroManualResponse> {
    return this.http.post<RegistroManualResponse>(`${this.apiUrl}/registro-manual`, formData);
  }
}
