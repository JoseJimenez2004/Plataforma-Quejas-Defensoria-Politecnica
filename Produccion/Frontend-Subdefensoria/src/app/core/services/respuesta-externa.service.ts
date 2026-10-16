import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { RespuestaExterna, RegistrarRespuestaExternaPayload } from '../models/respuesta-externa';

@Injectable({ providedIn: 'root' })
export class RespuestaExternaService {
  constructor(private api: ApiService) {}

  /** Con archivo (opcional): una sola petición multipart con "datos" (JSON) y "archivo". */
  registrarRespuesta(payload: RegistrarRespuestaExternaPayload, archivo?: File | null): Observable<RespuestaExterna> {
    if (!archivo) {
      return this.api.post<RespuestaExterna>('/respuestas-externas', payload);
    }
    const datos = new FormData();
    datos.append('datos', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    datos.append('archivo', archivo, archivo.name);
    return this.api.post<RespuestaExterna>('/respuestas-externas', datos);
  }

  /** Adjunta o reemplaza el archivo de una respuesta ya registrada. */
  adjuntarArchivo(respuestaId: number, archivo: File): Observable<RespuestaExterna> {
    const datos = new FormData();
    datos.append('archivo', archivo, archivo.name);
    return this.api.post<RespuestaExterna>(`/respuestas-externas/${respuestaId}/archivo`, datos);
  }

  porOficio(oficioId: number): Observable<RespuestaExterna[]> {
    return this.api.get<RespuestaExterna[]>(`/respuestas-externas/oficio/${oficioId}`);
  }

  archivo(respuestaId: number): Observable<Blob> {
    return this.api.getBlob(`/respuestas-externas/${respuestaId}/archivo`);
  }
}
