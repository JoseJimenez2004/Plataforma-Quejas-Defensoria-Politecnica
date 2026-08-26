import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Recordatorio, GenerarRecordatorioPayload } from '../models/recordatorio';

@Injectable({ providedIn: 'root' })
export class RecordatorioService {
  constructor(private api: ApiService) {}

  generarRecordatorio(payload: GenerarRecordatorioPayload): Observable<Recordatorio> {
    return this.api.post<Recordatorio>('/recordatorios', payload);
  }
}
