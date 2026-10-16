import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DatosVentanilla, Queja, RegistroQuejaPublicaRequest } from '../models/queja-publica.models';

/**
 * Registro de una queja que llega a ventanilla (2026-10-16). Usa el MISMO formulario y el
 * MISMO endpoint que el portal público, así la queja pasa por las mismas validaciones
 * (credencial, archivos, fechas, correo) y recibe su folio igual. Después revision-service la
 * marca como recibida en ventanilla y guarda los datos del documento físico.
 */
@Injectable({ providedIn: 'root' })
export class QuejaVentanillaService {
  constructor(private http: HttpClient) {}

  registrarQuejaPublica(datos: RegistroQuejaPublicaRequest): Observable<Queja> {
    const formData = new FormData();
    formData.append('nombre', datos.nombre);
    formData.append('apellido1', datos.apellido1);
    if (datos.apellido2) {
      formData.append('apellido2', datos.apellido2);
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
    if (datos.apellido1Denunciado) {
      formData.append('apellido1Denunciado', datos.apellido1Denunciado);
    }
    if (datos.apellido2Denunciado) {
      formData.append('apellido2Denunciado', datos.apellido2Denunciado);
    }
    formData.append('descripcion', datos.descripcion);
    formData.append('avisoPrivacidadAceptado', String(datos.avisoPrivacidadAceptado));
    formData.append('avisoPrivacidadVersion', datos.avisoPrivacidadVersion);
    for (const archivo of datos.archivos ?? []) {
      formData.append('archivos', archivo);
    }
    if (datos.tutor) {
      formData.append('tutorNombre', datos.tutor.nombre);
      formData.append('tutorApellido1', datos.tutor.apellido1);
      if (datos.tutor.apellido2) {
        formData.append('tutorApellido2', datos.tutor.apellido2);
      }
      formData.append('tutorParentesco', datos.tutor.parentesco);
      if (datos.tutor.correo) {
        formData.append('tutorCorreo', datos.tutor.correo);
      }
      if (datos.tutor.telefono) {
        formData.append('tutorTelefono', datos.tutor.telefono);
      }
    }
    return this.http.post<Queja>('/api/quejoso/quejas/registro-publico', formData);
  }

  /** Marca la queja recién creada como recibida en ventanilla (origen MANUAL). */
  marcarVentanilla(folio: string, datos: DatosVentanilla): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`/api/revision/registro-manual/${encodeURIComponent(folio)}/ventanilla`, datos);
  }
}
