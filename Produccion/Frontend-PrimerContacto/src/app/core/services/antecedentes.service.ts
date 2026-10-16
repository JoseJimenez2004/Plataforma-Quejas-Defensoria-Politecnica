import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { AntecedenteGuardado, BusquedaAntecedentes } from '../models/antecedentes.model';

/**
 * Antecedentes de un expediente: búsqueda con el modelo (el backend cae al motor por reglas
 * si el modelo no responde), búsqueda manual por quejoso/denunciado, y los antecedentes
 * finales que el analista elige guardar.
 */
@Injectable({
  providedIn: 'root'
})
export class AntecedentesService {

  constructor(private api: ApiService) {}

  buscar(folio: string): Observable<BusquedaAntecedentes> {
    return this.api.get<BusquedaAntecedentes>(
      `/antecedentes/${encodeURIComponent(folio)}`
    );
  }

  buscarManual(folio: string, quejoso: string, denunciado: string): Observable<BusquedaAntecedentes> {
    const params = new URLSearchParams();
    if (quejoso.trim()) params.set('quejoso', quejoso.trim());
    if (denunciado.trim()) params.set('denunciado', denunciado.trim());

    return this.api.get<BusquedaAntecedentes>(
      `/antecedentes/${encodeURIComponent(folio)}/manual?${params.toString()}`
    );
  }

  guardados(folio: string): Observable<AntecedenteGuardado[]> {
    return this.api.get<AntecedenteGuardado[]>(
      `/antecedentes/${encodeURIComponent(folio)}/guardados`
    );
  }

  /** Guarda la selección; devuelve la lista completa de guardados. */
  guardar(folio: string, antecedentes: AntecedenteGuardado[]): Observable<AntecedenteGuardado[]> {
    return this.api.post<AntecedenteGuardado[]>(
      `/antecedentes/${encodeURIComponent(folio)}/guardados`,
      { antecedentes }
    );
  }

  quitar(folio: string, id: number): Observable<void> {
    return this.api.delete<void>(
      `/antecedentes/${encodeURIComponent(folio)}/guardados/${id}`
    );
  }
}
