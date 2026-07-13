import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Queja, ValidarFolioRequest } from '../models/queja.models';

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

  /** Registra una queja nueva. Requiere sesión iniciada (el interceptor JWT
   * adjunta el token). El backend espera multipart/form-data, no JSON. */
  registrarQueja(motivo: string, descripcion: string, archivo?: File | null): Observable<Queja> {
    const formData = new FormData();
    formData.append('motivo', motivo);
    formData.append('descripcion', descripcion);
    if (archivo) {
      formData.append('archivo', archivo);
    }
    return this.http.post<Queja>(`${this.apiUrl}/registrar`, formData);
  }
}
