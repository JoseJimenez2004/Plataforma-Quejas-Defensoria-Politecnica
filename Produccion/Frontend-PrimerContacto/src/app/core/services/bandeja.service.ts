import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiService } from './api.service';
import { ExpedienteBandeja, FiltroBandeja } from '../models/expediente-bandeja';
import {
  etiquetaEstatus,
  codigoEstatus,
  formatearFecha,
  formatearPrioridad
} from '../utils/estatus-expediente';

interface BandejaBackendDTO {
  expedienteId: number;
  folio: string;
  folioOrigen?: string;

  nombreQuejoso: string;
  unidadAcademica: string;
  tema: string;
  prioridad: string;
  estatus: string;
  fechaRecepcion: string;
  tieneCitaActiva: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BandejaService {

  constructor(private api: ApiService) {}

  obtenerBandeja(): Observable<ExpedienteBandeja[]> {
    return this.api.get<BandejaBackendDTO[]>('/bandeja').pipe(
      map(items => items.map(item => this.mapearExpediente(item)))
    );
  }

  /**
   * CU-PC-02: el filtrado lo hace el backend (POST /bandeja/filtrar). Los valores de
   * prioridad y estatus se mandan como códigos del backend (ALTA, EN_ANALISIS...).
   */
  filtrar(filtro: FiltroBandeja): Observable<ExpedienteBandeja[]> {
    return this.api.post<BandejaBackendDTO[]>('/bandeja/filtrar', filtro).pipe(
      map(items => items.map(item => this.mapearExpediente(item)))
    );
  }

  buscarPorFolio(folio: string): Observable<ExpedienteBandeja> {
    return this.api
      .get<BandejaBackendDTO>(`/bandeja/folio/${folio}`)
      .pipe(
        map(item => this.mapearExpediente(item))
      );
  }

  private mapearExpediente(
    item: BandejaBackendDTO
  ): ExpedienteBandeja {

    return {
      expedienteId: item.expedienteId,
      folio: item.folio,
      folioOrigen: item.folioOrigen,

      nombreQuejoso: item.nombreQuejoso ?? '',
      unidadAcademica: item.unidadAcademica ?? '',
      tema: item.tema ?? '',

      prioridad: formatearPrioridad(item.prioridad),
      estatus: etiquetaEstatus(item.estatus),
      estatusCodigo: codigoEstatus(item.estatus),
      fechaRecepcion: formatearFecha(item.fechaRecepcion),
      tieneCitaActiva: !!item.tieneCitaActiva
    };
  }
}
