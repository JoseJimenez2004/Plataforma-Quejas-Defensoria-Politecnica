import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { AcuerdoConclusion, CrearAcuerdoConclusionPayload } from '../models/acuerdo-conclusion';

@Injectable({ providedIn: 'root' })
export class AcuerdoConclusionService {
  constructor(private api: ApiService) {}

  guardarOConcluir(payload: CrearAcuerdoConclusionPayload): Observable<AcuerdoConclusion> {
    return this.api.post<AcuerdoConclusion>('/acuerdos-conclusion', payload);
  }

  obtenerPorExpediente(expedienteId: number): Observable<AcuerdoConclusion> {
    return this.api.get<AcuerdoConclusion>(`/acuerdos-conclusion/expediente/${expedienteId}`);
  }
}
