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
}

@Injectable({
  providedIn: 'root'
})
export class ResumenService {

  constructor(private api: ApiService) {}

  /** Puede tardar hasta ~2 minutos; responde 503 si el modelo no está disponible. */
  resumir(texto: string): Observable<ResumenModelo> {
    return this.api.post<ResumenModelo>('/resumen', { texto });
  }
}
