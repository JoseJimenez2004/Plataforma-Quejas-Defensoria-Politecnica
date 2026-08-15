import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AcuerdoConciliacion, RespuestaConciliacionRequest } from '../models/conciliacion.models';

@Injectable({ providedIn: 'root' })
export class ConciliacionService {
  private readonly apiUrl = '/api/quejoso/conciliaciones';

  constructor(private http: HttpClient) {}

  misAcuerdos(): Observable<AcuerdoConciliacion[]> {
    return this.http.get<AcuerdoConciliacion[]>(`${this.apiUrl}/mias`);
  }

  responder(id: number, datos: RespuestaConciliacionRequest): Observable<AcuerdoConciliacion> {
    return this.http.put<AcuerdoConciliacion>(`${this.apiUrl}/${id}/respuesta`, datos);
  }
}
