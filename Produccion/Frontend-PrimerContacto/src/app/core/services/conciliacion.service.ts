import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import {
  AcuerdoConciliacion,
  CrearConciliacionPayload
} from '../models/conciliacion.model';

/** CU-PC-10: acuerdos de conciliación propuestos por Primer Contacto. */
@Injectable({
  providedIn: 'root'
})
export class ConciliacionService {

  constructor(private api: ApiService) {}

  /**
   * Acuerdos del expediente, del más reciente al más antiguo. Si el quejoso ya aceptó uno,
   * el backend turna el expediente a Subdefensoría en esta misma consulta.
   */
  listar(folio: string): Observable<AcuerdoConciliacion[]> {
    return this.api.get<AcuerdoConciliacion[]>(
      `/conciliaciones/expediente/${encodeURIComponent(folio)}`
    );
  }

  proponer(dto: CrearConciliacionPayload): Observable<AcuerdoConciliacion> {
    return this.api.post<AcuerdoConciliacion>('/conciliaciones', dto);
  }
}
