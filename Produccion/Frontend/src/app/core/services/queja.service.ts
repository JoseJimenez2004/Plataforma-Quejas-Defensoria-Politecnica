import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  EvidenciaResumen,
  Queja,
  RegistroQuejaPublicaRequest,
  ValidarFolioRequest,
} from '../models/queja.models';

/** Datos estructurados de una queja registrada por un usuario autenticado — antes
 * "unidadAcademica"/"fechaHechos"/denunciado se combinaban a mano dentro de la descripción;
 * ahora se mandan como campos propios que el backend guarda en columnas separadas. */
export interface DatosQuejaAutenticada {
  motivo: string;
  descripcion: string;
  unidadAcademicaClave?: string;
  fechaHechos?: string;
  nombreDenunciado?: string;
  apellidoDenunciado?: string;
  archivos?: File[];
}

@Injectable({ providedIn: 'root' })
export class QuejaService {
  private readonly apiUrl = '/api/quejoso/quejas';

  constructor(private http: HttpClient) {}

  /** Confirma si un folio + correo corresponden a una queja real. Es lo único
   * que el backend expone hoy para "seguimiento sin cuenta" — no regresa el
   * detalle de la queja, solo true/false. Ver docs/HALLAZGOS.md. */
  validarFolio(datos: ValidarFolioRequest): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/validar-folio`, datos);
  }

  /** Detalle público de una queja por folio + correo (sin sesión iniciada) — usado por
   * "Consultar folio" para mostrar los datos reales de la queja, no solo un true/false. */
  obtenerPorFolio(folio: string, correo: string): Observable<Queja> {
    const params = new URLSearchParams({ correo });
    return this.http.get<Queja>(
      `${this.apiUrl}/folio/${encodeURIComponent(folio)}?${params.toString()}`,
    );
  }

  /** Registra una queja nueva con 0 o más archivos de evidencia. Requiere sesión iniciada
   * (el interceptor JWT adjunta el token). El backend espera multipart/form-data, no JSON;
   * cada archivo se manda repitiendo el campo "archivos" (así Spring lo recibe como lista). */
  registrarQueja(datos: DatosQuejaAutenticada): Observable<Queja> {
    const formData = new FormData();
    formData.append('motivo', datos.motivo);
    formData.append('descripcion', datos.descripcion);
    if (datos.unidadAcademicaClave) {
      formData.append('unidadAcademicaClave', datos.unidadAcademicaClave);
    }
    if (datos.fechaHechos) {
      formData.append('fechaHechos', datos.fechaHechos);
    }
    if (datos.nombreDenunciado) {
      formData.append('nombreDenunciado', datos.nombreDenunciado);
    }
    if (datos.apellidoDenunciado) {
      formData.append('apellidoDenunciado', datos.apellidoDenunciado);
    }
    for (const archivo of datos.archivos ?? []) {
      formData.append('archivos', archivo);
    }
    return this.http.post<Queja>(`${this.apiUrl}/registrar`, formData);
  }

  /** Registra una queja desde el formulario público, sin sesión iniciada — nuevo endpoint
   * /registro-publico (no requiere JWT, ver WebConfig del backend). Manda la identidad
   * completa del quejoso (y del tutor, si aplica) en vez de derivar el correo de un token. */
  registrarQuejaPublica(datos: RegistroQuejaPublicaRequest): Observable<Queja> {
    const formData = new FormData();
    formData.append('nombre', datos.nombre);
    formData.append('apellidoPaterno', datos.apellidoPaterno);
    if (datos.apellidoMaterno) {
      formData.append('apellidoMaterno', datos.apellidoMaterno);
    }
    formData.append('correo', datos.correo);
    formData.append('fechaNacimiento', datos.fechaNacimiento);
    formData.append('tipoIdentificacion', datos.tipoIdentificacion);
    formData.append('numeroIdentificacion', datos.numeroIdentificacion);
    formData.append('unidadAcademicaClave', datos.unidadAcademicaClave);
    formData.append('fechaHechos', datos.fechaHechos);
    if (datos.nombreDenunciado) {
      formData.append('nombreDenunciado', datos.nombreDenunciado);
    }
    if (datos.apellidoDenunciado) {
      formData.append('apellidoDenunciado', datos.apellidoDenunciado);
    }
    formData.append('descripcion', datos.descripcion);
    for (const archivo of datos.archivos ?? []) {
      formData.append('archivos', archivo);
    }
    if (datos.tutor) {
      formData.append('tutorNombre', datos.tutor.nombre);
      formData.append('tutorApellidoPaterno', datos.tutor.apellidoPaterno);
      if (datos.tutor.apellidoMaterno) {
        formData.append('tutorApellidoMaterno', datos.tutor.apellidoMaterno);
      }
      formData.append('tutorParentesco', datos.tutor.parentesco);
      if (datos.tutor.correo) {
        formData.append('tutorCorreo', datos.tutor.correo);
      }
      if (datos.tutor.telefono) {
        formData.append('tutorTelefono', datos.tutor.telefono);
      }
    }
    return this.http.post<Queja>(`${this.apiUrl}/registro-publico`, formData);
  }

  /** Lista todas las quejas del usuario autenticado (panel "Resumen" / "Mis Quejas"). */
  misQuejas(): Observable<Queja[]> {
    return this.http.get<Queja[]>(`${this.apiUrl}/mias`);
  }

  /** Detalle de una queja propia del usuario autenticado (panel "Ver / Editar"). */
  miQuejaPorFolio(folio: string): Observable<Queja> {
    return this.http.get<Queja>(`${this.apiUrl}/mias/${encodeURIComponent(folio)}`);
  }

  /** Evidencias (solo metadatos) de una queja propia. */
  misEvidencias(folio: string): Observable<EvidenciaResumen[]> {
    return this.http.get<EvidenciaResumen[]>(
      `${this.apiUrl}/mias/${encodeURIComponent(folio)}/evidencias`,
    );
  }
}
