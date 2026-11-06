import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiService } from './api.service';
import { RespuestaDenunciado, SolicitudDenunciado, VersionDenunciadoCaptura } from '../models/denunciado.model';

/**
 * Versión del denunciado:
 *  - PC-2 (2026-11): la captura el analista tras entrevistar al denunciado (ya no hay enlace);
 *  - la respuesta la guarda denunciado-service por folio de la queja (FOL-...).
 *  Las solicitudes por correo anteriores se siguen listando solo como historial.
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

  /**
   * PC-2: el analista registra la versión del denunciado (ya no se le manda enlace).
   * POST /api/denunciado/respuestas/personal (multipart, con sesión).
   */
  registrarVersion(v: VersionDenunciadoCaptura): Observable<{ folioRespuesta: string }> {
    const datos = new FormData();
    datos.append('folioQueja', v.folioQueja);
    datos.append('nombre', v.nombre);
    datos.append('apellido1', v.apellido1);
    if (v.apellido2) datos.append('apellido2', v.apellido2);
    datos.append('unidadProcedenciaClave', v.unidadProcedenciaClave);
    datos.append('tipoIdentificacion', v.tipoIdentificacion);
    datos.append('numeroIdentificacion', v.numeroIdentificacion);
    if (v.fechaEntrevista) datos.append('fechaEntrevista', v.fechaEntrevista);
    datos.append('descripcionHechos', v.descripcionHechos);
    v.credencial.forEach(f => datos.append('credencial', f, f.name));
    v.evidencias.forEach(f => datos.append('evidencias', f, f.name));
    return this.http.post<{ folioRespuesta: string }>(`${this.denunciadoUrl}/respuestas/personal`, datos);
  }

  /** Credencial o evidencia de la respuesta, para abrirla en otra pestaña. */
  descargarArchivo(id: number): Observable<Blob> {
    return this.http.get(`${this.denunciadoUrl}/respuestas/archivos/${id}`, { responseType: 'blob' });
  }
}
