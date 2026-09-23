import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import {
  Dictamen,
  CompetenciaPayload,
  ImprocedenciaPayload
} from '../models/dictamen.model';

@Injectable({
  providedIn: 'root',
})
export class DictamenService {

  constructor(private api: ApiService) {}

  registrarCompetencia(
    dto: CompetenciaPayload
  ): Observable<Dictamen> {

    return this.api.post<Dictamen>(
      '/dictamenes/competente',
      dto
    );
  }

  registrarImprocedencia(
    dto: ImprocedenciaPayload
  ): Observable<Dictamen> {

    return this.api.post<Dictamen>(
      '/dictamenes/improcedente',
      dto
    );
  }

  obtenerPorFolio(
    folio: string
  ): Observable<Dictamen> {

    return this.api.get<Dictamen>(
      `/dictamenes/folio/${encodeURIComponent(folio)}`
    );
  }
}