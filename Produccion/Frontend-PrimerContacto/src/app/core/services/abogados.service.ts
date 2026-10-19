import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';

/** Abogado que puede recibir el expediente en Subdefensoría, con su carga en trámite. */
export interface AbogadoSubdefensoria {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  enTramite: number;
}

@Injectable({ providedIn: 'root' })
export class AbogadosService {
  constructor(private api: ApiService) {}

  /** Del que menos quejas en trámite tiene al que más. */
  listar(): Observable<AbogadoSubdefensoria[]> {
    return this.api.get<AbogadoSubdefensoria[]>('/abogados');
  }
}
