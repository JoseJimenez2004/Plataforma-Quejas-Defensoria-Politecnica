import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { BusquedaAntecedentes } from '../models/antecedentes.model';

/**
 * Búsqueda de antecedentes de un expediente. El backend decide con qué motor buscar
 * (hoy reglas provisionales, después el modelo); esta interfaz no cambia.
 */
@Injectable({
  providedIn: 'root'
})
export class AntecedentesService {

  constructor(private api: ApiService) {}

  buscar(folio: string): Observable<BusquedaAntecedentes> {
    return this.api.get<BusquedaAntecedentes>(
      `/antecedentes/${encodeURIComponent(folio)}`
    );
  }
}
