import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import {
  Remision,
  CrearRemisionPayload,
  InstanciaRemision
} from '../models/remision.model';

@Injectable({
  providedIn: 'root'
})
export class RemisionService {

  constructor(private api: ApiService) {}

  /** OAG, OIC y COSECOVI / Género, con el correo al que saldrá el oficio. */
  obtenerInstancias(): Observable<InstanciaRemision[]> {
    return this.api.get<InstanciaRemision[]>('/remisiones/instancias');
  }

  /**
   * Genera el oficio y lo envía por correo a la instancia en un solo paso. Si el correo no
   * sale, el backend responde error y no registra nada.
   */
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

  /** Envía por correo una remisión que quedó GENERADA con el flujo anterior. */
  enviarRemision(
    folio: string,
    instanciaClave: string
  ): Observable<Remision> {

    return this.api.put<Remision>(
      `/remisiones/folio/${encodeURIComponent(folio)}/enviar`,
      { instanciaClave }
    );
  }
}
