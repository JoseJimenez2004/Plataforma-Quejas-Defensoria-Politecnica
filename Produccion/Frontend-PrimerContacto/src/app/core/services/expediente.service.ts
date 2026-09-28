import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { ExpedientePrimerContacto } from '../models/expediente-primer-contacto';
import { ExpedienteDetalle } from '../models/expediente-detalle';
import {
  codigoEstatus,
  etiquetaEstatus,
  formatearFecha,
  formatearPrioridad
} from '../utils/estatus-expediente';

@Injectable({
  providedIn: 'root'
})
export class ExpedienteService {

  constructor(private api: ApiService) {}

  obtenerPorFolio(
    folio: string
  ): Observable<ExpedientePrimerContacto> {

    return this.api.get<ExpedientePrimerContacto>(
      `/expedientes/folio/${encodeURIComponent(folio)}`
    );
  }

  /**
   * CU-PC-03: el analista abre el expediente para trabajarlo. Si estaba TURNADA pasa a
   * EN_ANALISIS; en cualquier otro estado solo lo devuelve. Solo lo llama la pantalla de
   * detalle: Agenda, Dictamen y Remisión consultan con obtenerPorFolio().
   */
  iniciarAnalisis(
    folio: string
  ): Observable<ExpedientePrimerContacto> {

    return this.api.post<ExpedientePrimerContacto>(
      `/expedientes/folio/${encodeURIComponent(folio)}/iniciar-analisis`,
      {}
    );
  }

  /** Archivo real de una evidencia (CU-PC-06/07). */
  descargarEvidencia(id: number): Observable<Blob> {
    return this.api.getBlob(`/evidencias/${id}`);
  }

  /**
   * Adapta el expediente del backend al modelo usado por
   * las pantallas de expediente, dictamen y remisión.
   */
  mapearADetalle(
    exp: ExpedientePrimerContacto
  ): ExpedienteDetalle {

    return {
      expedienteId: exp.expedienteId,
      folio: exp.folio,
      folioOrigen: exp.folioOrigen,
      folioSubdefensoria: exp.folioSubdefensoria,

      asunto: exp.tema ?? '',
      fechaIngreso: formatearFecha(exp.fechaRecepcion),

      estatus: etiquetaEstatus(exp.estatus),
      estatusCodigo: codigoEstatus(exp.estatus),

      prioridad: formatearPrioridad(exp.prioridad),

      narrativa: exp.descripcionHechos ?? '',

      fechaInicioAnalisis: exp.fechaInicioAnalisis,
      analistaAnalisisNombre: exp.analistaAnalisisNombre,

      quejoso: {
        nombre: exp.quejoso?.nombreCompleto ?? '',
        boleta: exp.quejoso?.tipoUsuario ?? '',
        correo: exp.quejoso?.correo ?? '',
        telefono: exp.quejoso?.telefono ?? '',
        unidadAcademica:
          exp.quejoso?.unidadAcademica ?? ''
      },

      evidencias: (exp.evidencias ?? []).map(
        evidencia => ({
          id: evidencia.id,
          nombre: evidencia.nombreArchivo,
          tipo: evidencia.tipoArchivo
        })
      ),

      notas: exp.notas ?? []
    };
  }
}
