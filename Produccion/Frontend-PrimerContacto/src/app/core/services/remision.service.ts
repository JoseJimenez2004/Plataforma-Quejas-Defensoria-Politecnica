import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import {
  Remision,
  CrearRemisionPayload
} from '../models/remision.model';

@Injectable({
  providedIn: 'root'
})
export class RemisionService {

  constructor(private api: ApiService) {}

  /** Deja la remisión GENERADA: el oficio ya se puede descargar. */
  crearRemision(
    dto: CrearRemisionPayload
  ): Observable<Remision> {

    return this.api.post<Remision>(
      '/remisiones',
      dto
    );
  }

  /** 404 si el expediente todavía no tiene remisión. */
  obtenerPorFolio(
    folio: string
  ): Observable<Remision> {

    return this.api.get<Remision>(
      `/remisiones/folio/${encodeURIComponent(folio)}`
    );
  }

  descargarPdf(
    folio: string
  ): Observable<Blob> {

    return this.api.getBlob(
      `/remisiones/folio/${encodeURIComponent(folio)}/pdf`
    );
  }

  /** Registra que el oficio se envió: remisión ENVIADA y expediente REMITIDA. */
  enviarRemision(
    folio: string
  ): Observable<Remision> {

    return this.api.put<Remision>(
      `/remisiones/folio/${encodeURIComponent(folio)}/enviar`,
      {}
    );
  }
}
