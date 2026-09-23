import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiService } from './api.service';
import {
  CitaPrimerContacto,
  CrearCitaPrimerContacto
} from '../models/cita-primer-contacto';

interface CitaBackendDTO {
  id: number;
  expedienteId: number;
  folio: string;

  quejosoId?: number;
  quejosoNombre?: string;

  analistaId: number;
  analistaNombre: string;

  fechaCita: string;
  horaCita: string;

  tipoCita: string;
  motivo: string;
  estatus: string;

  fechaCreacion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AgendaService {

  constructor(private api: ApiService) {}

  crearCita(
    dto: CrearCitaPrimerContacto
  ): Observable<CitaPrimerContacto> {

    return this.api
      .post<CitaBackendDTO>('/citas', dto)
      .pipe(
        map(cita => this.mapearCita(cita))
      );
  }

  obtenerAgendaDia(
    fecha: string
  ): Observable<CitaPrimerContacto[]> {

    return this.api
      .get<CitaBackendDTO[]>(
        `/citas/agenda?fecha=${encodeURIComponent(fecha)}`
      )
      .pipe(
        map(citas =>
          citas.map(cita => this.mapearCita(cita))
        )
      );
  }

  listarPorFolio(
    folio: string
  ): Observable<CitaPrimerContacto[]> {

    return this.api
      .get<CitaBackendDTO[]>(
        `/citas/folio/${encodeURIComponent(folio)}`
      )
      .pipe(
        map(citas =>
          citas.map(cita => this.mapearCita(cita))
        )
      );
  }

  cancelarCita(
    id: number
  ): Observable<CitaPrimerContacto> {

    return this.api
      .put<CitaBackendDTO>(
        `/citas/${id}/cancelar`,
        {}
      )
      .pipe(
        map(cita => this.mapearCita(cita))
      );
  }

  private mapearCita(
    cita: CitaBackendDTO
  ): CitaPrimerContacto {

    return {
      id: cita.id,
      expedienteId: cita.expedienteId,
      folio: cita.folio,

      quejosoId: cita.quejosoId,
      quejoso: cita.quejosoNombre ?? '',

      analistaId: cita.analistaId,
      analistaNombre: cita.analistaNombre,

      fecha: this.formatearFechaVista(
        cita.fechaCita
      ),

      hora: cita.horaCita?.slice(0, 5),

      tipo:
        cita.tipoCita === 'VIRTUAL'
          ? 'Virtual'
          : 'Presencial',

      motivo: cita.motivo,

      estatus: this.formatearEstatus(
        cita.estatus
      ),

      fechaCreacion: cita.fechaCreacion
    };
  }

  private formatearFechaVista(
    fecha: string
  ): string {

    if (!fecha) {
      return '';
    }

    const [year, month, day] = fecha.split('-');

    return `${day}/${month}/${year}`;
  }

  private formatearEstatus(
    estatus: string
  ): string {

    switch (estatus?.toUpperCase()) {

      case 'PROGRAMADA':
        return 'Programada';

      case 'CONFIRMADA':
        return 'Confirmada';

      case 'CANCELADA':
        return 'Cancelada';

      default:
        return estatus;
    }
  }
}