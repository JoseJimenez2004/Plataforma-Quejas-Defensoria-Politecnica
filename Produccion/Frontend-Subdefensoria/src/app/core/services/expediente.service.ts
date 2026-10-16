import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ExpedienteInvestigacion } from '../models/expediente-investigacion';

@Injectable({ providedIn: 'root' })
export class ExpedienteService {
  constructor(private api: ApiService) {}

  obtenerPorFolio(folio: string): Observable<ExpedienteInvestigacion> {
    return this.api.get<ExpedienteInvestigacion>(`/expedientes/folio/${folio}`);
  }

  /** Al abrir el detalle: RECIBIDO pasa a EN_INVESTIGACION (en otro estado solo lo devuelve). */
  iniciarInvestigacion(folio: string): Observable<ExpedienteInvestigacion> {
    return this.api.post<ExpedienteInvestigacion>(`/expedientes/folio/${folio}/iniciar-investigacion`, {});
  }
}
