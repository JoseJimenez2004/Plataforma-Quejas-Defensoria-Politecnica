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

  /** Solo el autor de la nota puede editarla (el backend responde 403 si no). */
  actualizarNota(
    id: number,
    dto: CrearNotaAnalisis
  ): Observable<NotaAnalisis> {

    return this.api.put<NotaAnalisis>(`/notas/${id}`, dto);
  }

  /** Solo el autor de la nota puede eliminarla (el backend responde 403 si no). */
  eliminarNota(id: number): Observable<void> {
    return this.api.delete<void>(`/notas/${id}`);
  }
}
