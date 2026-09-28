import { Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { ApiService } from './api.service';

export interface AnalistaSesion {
  id: number;
  nombreCompleto: string;
  correo: string;
}

/**
 * Analista autenticado según el backend (GET /analistas/yo). Se usa para saber qué notas
 * son del analista actual y mostrarle editar/eliminar solo en esas (CU-PC-05).
 */
@Injectable({
  providedIn: 'root'
})
export class AnalistaSesionService {

  private analista$?: Observable<AnalistaSesion>;

  constructor(private api: ApiService) {}

  obtener(): Observable<AnalistaSesion> {
    this.analista$ ??= this.api.get<AnalistaSesion>('/analistas/yo').pipe(shareReplay(1));
    return this.analista$;
  }
}
