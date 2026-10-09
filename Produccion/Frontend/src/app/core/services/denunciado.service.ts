import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { RespuestaDenunciadoRequest, RespuestaRegistrada } from '../models/denunciado.models';

/** Cliente de denunciado-service (puerto 8094, ruta pública /api/denunciado). */
@Injectable({ providedIn: 'root' })
export class DenunciadoService {
  private readonly apiUrl = '/api/denunciado/respuestas';

  constructor(private http: HttpClient) {}

  registrarRespuesta(
    datos: RespuestaDenunciadoRequest,
    credencial: File[],
    evidencias: File[],
  ): Observable<RespuestaRegistrada> {
    const formData = new FormData();
    formData.append('folioQueja', datos.folioQueja);
    formData.append('nombre', datos.nombre);
    formData.append('apellido1', datos.apellido1);
    if (datos.apellido2) {
      formData.append('apellido2', datos.apellido2);
    }
    formData.append('unidadProcedenciaClave', datos.unidadProcedenciaClave);
    formData.append('tipoIdentificacion', datos.tipoIdentificacion);
    formData.append('numeroIdentificacion', datos.numeroIdentificacion);
    formData.append('descripcionHechos', datos.descripcionHechos);
    formData.append('avisoPrivacidadAceptado', 'true');
    formData.append('avisoPrivacidadVersion', datos.avisoPrivacidadVersion);
    credencial.forEach((archivo) => formData.append('credencial', archivo, archivo.name));
    evidencias.forEach((archivo) => formData.append('evidencias', archivo, archivo.name));
    return this.http.post<RespuestaRegistrada>(this.apiUrl, formData);
  }
}
