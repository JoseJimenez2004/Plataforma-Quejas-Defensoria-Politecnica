import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiService } from './api.service';
import { RespuestaDenunciado, SolicitudDenunciado } from '../models/denunciado.model';

/**
 * Versión del denunciado:
 *  - la solicitud (correo con el enlace al formulario) la manda Primer Contacto;
 *  - la respuesta la guarda denunciado-service por folio de la queja (FOL-...).
 */
@Injectable({
  providedIn: 'root'
})
export class DenunciadoService {

  private readonly denunciadoUrl = environment.denunciadoApiUrl;

  constructor(
    private api: ApiService,
    private http: HttpClient
  ) {}

  listarSolicitudes(folio: string): Observable<SolicitudDenunciado[]> {
    return this.api.get<SolicitudDenunciado[]>(
      `/expedientes/folio/${encodeURIComponent(folio)}/denunciado/solicitudes`
    );
  }

  enviarSolicitud(folio: string, correo: string): Observable<SolicitudDenunciado> {
    return this.api.post<SolicitudDenunciado>(
      `/expedientes/folio/${encodeURIComponent(folio)}/denunciado/solicitudes`,
      { correo }
    );
  }

  listarRespuestas(folioQueja: string): Observable<RespuestaDenunciado[]> {
    return this.http.get<RespuestaDenunciado[]>(`${this.denunciadoUrl}/respuestas`, {
      params: new HttpParams().set('folioQueja', folioQueja)
    });
  }

  /** Credencial o evidencia de la respuesta, para abrirla en otra pestaña. */
  descargarArchivo(id: number): Observable<Blob> {
    return this.http.get(`${this.denunciadoUrl}/respuestas/archivos/${id}`, { responseType: 'blob' });
  }
}
