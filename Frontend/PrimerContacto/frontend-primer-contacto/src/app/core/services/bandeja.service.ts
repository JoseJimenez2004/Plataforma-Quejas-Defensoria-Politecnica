import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiService } from './api.service';
import { ExpedienteBandeja } from '../models/expediente-bandeja';

interface BandejaBackendDTO {
  quejaId: number;
  folio: string;
  nombreQuejoso: string;
  unidadAcademica: string;
  tema: string;
  prioridad: string;
  estatus: string;
  fechaRecepcion: string;
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

  buscarPorFolio(folio: string): Observable<ExpedienteBandeja> {
    return this.api
      .get<BandejaBackendDTO>(`/bandeja/folio/${folio}`)
      .pipe(map(item => this.mapearExpediente(item)));
  }

  private mapearExpediente(item: BandejaBackendDTO): ExpedienteBandeja {
    return {
      quejaId: item.quejaId,
      folio: item.folio,
      nombreQuejoso: item.nombreQuejoso,
      unidadAcademica: item.unidadAcademica,
      tema: item.tema,
      prioridad: this.formatearPrioridad(item.prioridad),
      estatus: this.formatearEstatus(item.estatus),
      fechaRecepcion: this.formatearFecha(item.fechaRecepcion)
    };
  }

  private formatearPrioridad(
    prioridad: string
  ): 'Alta' | 'Media' | 'Baja' {
    switch (prioridad?.toUpperCase()) {
      case 'ALTA':
        return 'Alta';

      case 'BAJA':
        return 'Baja';

      case 'MEDIA':
      default:
        return 'Media';
    }
  }

  private formatearEstatus(estatus: string): string {
    switch (estatus?.toUpperCase()) {
      case 'PENDIENTE':
      case 'PENDIENTE_ANALISIS':
        return 'Pendiente';

      case 'EN_ANALISIS':
        return 'En análisis';

      case 'COMPETENTE':
        return 'Competente';

      case 'IMPROCEDENTE':
        return 'Improcedente';

      case 'REMITIDA':
        return 'Remitida';

      default:
        return estatus;
    }
  }

  private formatearFecha(fecha: string): string {
    if (!fecha) return '';

    const [year, month, day] = fecha.split('-');

    return `${day}/${month}/${year}`;
  }
}