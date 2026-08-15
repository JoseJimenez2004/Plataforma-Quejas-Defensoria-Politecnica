import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PersonalCreadoResponse,
  PersonalRequest,
  PersonalResumen,
  ResetPasswordResponse,
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class PersonalService {
  private readonly apiUrl = '/api/admin/personal';

  constructor(private http: HttpClient) {}

  listar(): Observable<PersonalResumen[]> {
    return this.http.get<PersonalResumen[]>(this.apiUrl);
  }

  crear(datos: PersonalRequest): Observable<PersonalCreadoResponse> {
    return this.http.post<PersonalCreadoResponse>(this.apiUrl, datos);
  }

  editar(id: number, datos: PersonalRequest): Observable<PersonalResumen> {
    return this.http.put<PersonalResumen>(`${this.apiUrl}/${id}`, datos);
  }

  resetearPassword(id: number): Observable<ResetPasswordResponse> {
    return this.http.post<ResetPasswordResponse>(`${this.apiUrl}/${id}/resetear-password`, {});
  }

  darDeBaja(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  reactivar(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/reactivar`, {});
  }
}
