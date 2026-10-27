import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/*
 * Lo que trabajó Primer Contacto, de solo lectura (GET /api/primer-contacto/consulta/...).
 * Primer Contacto lo abre a los roles SUBDEFENSOR y DEFENSOR.
 */

export interface OficioPrimerContacto {
  id: number;
  numeroOficio: string;
  asunto: string | null;
  contenido: string;
  analistaNombre: string;
  fechaCreacion: string;
  fechaActualizacion?: string | null;
}

export interface EvidenciaPrimerContacto {
  id: number;
  nombreArchivo: string;
  tipoArchivo: string;
  fechaCarga: string;
}

export interface DictamenPrimerContacto {
  resultado: string;
  justificacion: string;
  observaciones: string | null;
  analistaNombre: string;
  responsableTurno: string | null;
  fechaDictamen: string;
}

export interface CitaPrimerContacto {
  id: number;
  fechaCita: string;
  horaCita: string;
  tipoCita: string;
  categoria: string | null;
  motivo: string | null;
  estatus: string;
  analistaNombre: string;
  motivoCancelacionQuejoso: string | null;
  fechaCreacion: string;
}

export interface ConsultaPrimerContacto {
  expediente: {
    folio: string;
    folioOrigen: string;
    tema: string;
    descripcionHechos: string;
    prioridad: string | null;
    fechaInicioAnalisis: string | null;
    analistaAnalisisNombre: string | null;
    quejoso: { nombreCompleto: string; correo: string | null; telefono: string | null; tipoUsuario: string | null } | null;
    datosQueja: {
      fechaRegistro: string | null;
      fechaHechos: string | null;
      denunciado: string | null;
      tipoIdentificacionQuejoso: string | null;
      numeroIdentificacionQuejoso: string | null;
    } | null;
    evidencias: EvidenciaPrimerContacto[];
    notas: OficioPrimerContacto[];
  };
  dictamen: DictamenPrimerContacto | null;
  citas: CitaPrimerContacto[];
}

@Injectable({ providedIn: 'root' })
export class PrimerContactoService {
  private readonly url = environment.primerContactoUrl;

  constructor(private http: HttpClient) {}

  /** folio = PC-... (folioOrigen del expediente de Subdefensoría). */
  consultar(folio: string): Observable<ConsultaPrimerContacto> {
    return this.http.get<ConsultaPrimerContacto>(`${this.url}/consulta/expedientes/${encodeURIComponent(folio)}`);
  }

  pdfOficio(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/notas/${id}/pdf`, { responseType: 'blob' });
  }

  evidencia(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/evidencias/${id}`, { responseType: 'blob' });
  }
}
