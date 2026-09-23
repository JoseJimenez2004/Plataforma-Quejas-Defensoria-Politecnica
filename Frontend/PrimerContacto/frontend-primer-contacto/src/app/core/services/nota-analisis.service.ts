import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import {
  CrearNotaAnalisis,
  NotaAnalisis
} from '../models/nota-analisis.model';

@Injectable({
  providedIn: 'root'
})
export class NotaAnalisisService {

  constructor(private api: ApiService) {}

  crearNota(
    dto: CrearNotaAnalisis
  ): Observable<NotaAnalisis> {

    return this.api.post<NotaAnalisis>(
      '/notas',
      dto
    );
  }

  obtenerPorFolio(
    folio: string
  ): Observable<NotaAnalisis[]> {

    return this.api.get<NotaAnalisis[]>(
      `/notas/folio/${encodeURIComponent(folio)}`
    );
  }
}