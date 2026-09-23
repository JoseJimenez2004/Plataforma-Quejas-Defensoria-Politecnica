import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { RespuestaExterna, RegistrarRespuestaExternaPayload } from '../models/respuesta-externa';

@Injectable({ providedIn: 'root' })
export class RespuestaExternaService {
  constructor(private api: ApiService) {}

  registrarRespuesta(payload: RegistrarRespuestaExternaPayload): Observable<RespuestaExterna> {
    return this.api.post<RespuestaExterna>('/respuestas-externas', payload);
  }
}
