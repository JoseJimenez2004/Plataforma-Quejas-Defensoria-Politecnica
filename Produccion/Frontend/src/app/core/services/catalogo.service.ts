import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Dependencia } from '../models/catalogo.models';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly apiUrl = '/api/catalogos';

  constructor(private http: HttpClient) {}

  /** Lista las dependencias activas del catálogo, opcionalmente filtradas por tipo
   * (ej. "Unidad Académica" para no mezclar CECyT/ESIME/etc. con divisiones
   * administrativas internas que no aplican como "lugar de los hechos"). */
  listarDependencias(tipo?: string): Observable<Dependencia[]> {
    const url = tipo
      ? `${this.apiUrl}/dependencias?tipo=${encodeURIComponent(tipo)}`
      : `${this.apiUrl}/dependencias`;
    return this.http.get<Dependencia[]>(url);
  }
}
