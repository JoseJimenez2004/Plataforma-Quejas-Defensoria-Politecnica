import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AntecedenteItem,
  AreaOpcion,
  BandejaResumen,
  DefensorOpcion,
  QuejaDetalle,
  RechazarQuejaRequest,
  RegistroManualResponse,
  TurnarQuejaRequest,
} from '../models/revision.models';

@Injectable({ providedIn: 'root' })
export class RevisionService {
  private readonly apiUrl = '/api/revision';

  constructor(private http: HttpClient) {}

  bandeja(): Observable<BandejaResumen> {
    return this.http.get<BandejaResumen>(`${this.apiUrl}/bandeja`);
  }

  detalle(folio: string): Observable<QuejaDetalle> {
    return this.http.get<QuejaDetalle>(`${this.apiUrl}/quejas/${folio}`);
  }

  antecedentes(folio: string): Observable<AntecedenteItem[]> {
    return this.http.get<AntecedenteItem[]>(`${this.apiUrl}/quejas/${folio}/antecedentes`);
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

  registrarManual(formData: FormData): Observable<RegistroManualResponse> {
    return this.http.post<RegistroManualResponse>(`${this.apiUrl}/registro-manual`, formData);
  }
}
