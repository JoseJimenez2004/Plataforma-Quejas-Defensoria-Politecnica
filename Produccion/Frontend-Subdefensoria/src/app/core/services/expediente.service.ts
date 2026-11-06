import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ExpedienteInvestigacion } from '../models/expediente-investigacion';

@Injectable({ providedIn: 'root' })
export class ExpedienteService {
  constructor(private api: ApiService) {}

  /** SD-6: el subdefensor cambia la prioridad (BAJA | MEDIA | ALTA | URGENTE). */
  cambiarPrioridad(id: number, prioridad: string): Observable<ExpedienteInvestigacion> {
    return this.api.patch<ExpedienteInvestigacion>(`/expedientes/${id}/prioridad`, { prioridad });
  }

  obtenerPorFolio(folio: string): Observable<ExpedienteInvestigacion> {
    return this.api.get<ExpedienteInvestigacion>(`/expedientes/folio/${folio}`);
  }

  /** Al abrir el detalle: RECIBIDO pasa a EN_INVESTIGACION (en otro estado solo lo devuelve). */
  iniciarInvestigacion(folio: string): Observable<ExpedienteInvestigacion> {
    return this.api.post<ExpedienteInvestigacion>(`/expedientes/folio/${folio}/iniciar-investigacion`, {});
  }
}
