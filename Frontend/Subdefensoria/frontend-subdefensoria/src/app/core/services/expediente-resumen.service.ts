import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ExpedienteResumen } from '../models/expediente-resumen';

@Injectable({ providedIn: 'root' })
export class ExpedienteResumenService {
  constructor(private api: ApiService) {}

  listarTodos(): Observable<ExpedienteResumen[]> {
    return this.api.get<ExpedienteResumen[]>('/expedientes');
  }
}
