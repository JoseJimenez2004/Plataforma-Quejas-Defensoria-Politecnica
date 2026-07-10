import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ExpedientePrimerContacto } from '../models/expediente-primer-contacto';
import { ExpedienteDetalle } from '../models/expediente-detalle';

@Injectable({
  providedIn: 'root'
})
export class ExpedienteService {

  constructor(private api: ApiService) {}

  obtenerPorFolio(folio: string): Observable<ExpedientePrimerContacto> {
    return this.api.get<ExpedientePrimerContacto>(`/expedientes/folio/${folio}`);
  }

  /**
   * Adapta el expediente tal como lo entrega el backend al formato de
   * presentación (ExpedienteDetalle) que usan las vistas de expediente,
   * dictamen y remisión.
   */
  mapearADetalle(exp: ExpedientePrimerContacto): ExpedienteDetalle {
    return {
      quejaId: exp.quejaId,
      folio: exp.folio,
      asunto: exp.descripcionHechos ?? '',
      fechaIngreso: this.formatearFecha(exp.fechaRecepcion),
      estatus: this.formatearEstatus(exp.estatus),
      prioridad: this.formatearPrioridad(exp.prioridad),
      narrativa: exp.descripcionHechos ?? '',
      quejoso: {
        nombre: exp.quejoso?.nombreCompleto ?? '',
        boleta: exp.quejoso?.tipoUsuario ?? '',
        correo: exp.quejoso?.correo ?? '',
        telefono: exp.quejoso?.telefono ?? '',
        unidadAcademica: exp.quejoso?.unidadAcademica ?? ''
      },
      evidencias: (exp.evidencias ?? []).map(ev => ({
        nombre: ev.nombreArchivo,
        tipo: ev.tipoArchivo
      })),
      notas: (exp.notas ?? []).map(n => n.contenido)
    };
  }

  private formatearPrioridad(prioridad: string): 'Alta' | 'Media' | 'Baja' {
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
