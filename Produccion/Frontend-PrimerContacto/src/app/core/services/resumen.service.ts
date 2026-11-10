import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';

/** Borrador de resumen del modelo (resumen-service, vía Primer Contacto). */
export interface ResumenModelo {
  resumen: string;
  /** Fragmentos de la queja que eligió el modelo, tal cual los escribió el quejoso. */
  fragmentos: string[];
  requiereRevision: boolean;
  versionPipeline?: string;
  /** Solo en los guardados: cuándo y quién lo generó. */
  fechaGeneracion?: string;
  generadoPor?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ResumenService {

  constructor(private api: ApiService) {}

  /** Resumen ya guardado de la queja; null si nadie lo ha generado (204). */
  guardado(folio: string): Observable<ResumenModelo | null> {
    return this.api.get<ResumenModelo | null>(`/resumen?folio=${encodeURIComponent(folio)}`);
  }

  /**
   * Con folio, el servidor lo genera UNA sola vez y lo guarda; las siguientes veces devuelve el
   * guardado. Puede tardar hasta ~2 minutos; responde 503 si el modelo no está disponible.
   */
  resumir(texto: string, folio?: string): Observable<ResumenModelo> {
    return this.api.post<ResumenModelo>('/resumen', { texto, folio });
  }
}
