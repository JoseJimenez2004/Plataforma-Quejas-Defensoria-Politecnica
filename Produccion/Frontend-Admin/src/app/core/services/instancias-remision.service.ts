import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Instancia a la que Primer Contacto remite (OAG, OIC, COSECOVI / Género). */
export interface InstanciaRemision {
  clave: string;
  sigla: string;
  nombre: string;
  correo?: string;
  orden: number;
  fechaActualizacion?: string;
  actualizadoPor?: string;
}

/** catalogo-service, solo rol DEFENSOR. Solo se cambia el correo. */
@Injectable({ providedIn: 'root' })
export class InstanciasRemisionService {
  private readonly apiUrl = '/api/catalogos/instancias-remision/admin';

  constructor(private http: HttpClient) {}

  listar(): Observable<InstanciaRemision[]> {
    return this.http.get<InstanciaRemision[]>(this.apiUrl);
  }

  cambiarCorreo(clave: string, correo: string): Observable<InstanciaRemision> {
    return this.http.put<InstanciaRemision>(`${this.apiUrl}/${encodeURIComponent(clave)}`, { correo });
  }
}
