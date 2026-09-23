import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { AlertaVencimiento } from '../models/alerta-vencimiento';

@Injectable({ providedIn: 'root' })
export class AlertaService {
  constructor(private api: ApiService) {}

  obtenerVencidos(): Observable<AlertaVencimiento[]> {
    return this.api.get<AlertaVencimiento[]>('/alertas/vencidos');
  }
}
