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

  /** 404 si el expediente todavía no tiene dictamen. */
  obtenerPorFolio(
    folio: string
  ): Observable<Dictamen> {

    return this.api.get<Dictamen>(
      `/dictamenes/folio/${encodeURIComponent(folio)}`
    );
  }

  /** Expediente PROCEDENTE que Subdefensoría no alcanzó a recibir. */
  reenviarASubdefensoria(
    folio: string
  ): Observable<Dictamen> {

    return this.api.post<Dictamen>(
      `/dictamenes/folio/${encodeURIComponent(folio)}/reenviar-subdefensoria`,
      {}
    );
  }
}
