import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BitacoraAccion, RespaldoResumen } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class SeguridadService {
  private readonly apiUrl = '/api/admin/seguridad';

  constructor(private http: HttpClient) {}

  listarRespaldos(): Observable<RespaldoResumen[]> {
    return this.http.get<RespaldoResumen[]>(`${this.apiUrl}/respaldos`);
  }

  respaldoManual(): Observable<RespaldoResumen> {
    return this.http.post<RespaldoResumen>(`${this.apiUrl}/respaldos/manual`, {});
  }

  urlDescarga(nombreArchivo: string): string {
    return `${this.apiUrl}/respaldos/${encodeURIComponent(nombreArchivo)}/descargar`;
  }

  restaurar(nombreArchivo: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/restaurar`, { confirmar: true, nombreArchivo });
  }

  bitacora(): Observable<BitacoraAccion[]> {
    return this.http.get<BitacoraAccion[]>(`${this.apiUrl}/bitacora`);
  }
}
