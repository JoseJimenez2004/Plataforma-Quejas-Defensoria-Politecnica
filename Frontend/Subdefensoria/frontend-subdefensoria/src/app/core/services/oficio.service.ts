import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Oficio, CrearOficioPayload } from '../models/oficio';

@Injectable({ providedIn: 'root' })
export class OficioService {
  constructor(private api: ApiService) {}

  crearOficio(payload: CrearOficioPayload): Observable<Oficio> {
    return this.api.post<Oficio>('/oficios', payload);
  }

  historialPorFolio(folio: string): Observable<Oficio[]> {
    return this.api.get<Oficio[]>(`/oficios/folio/${folio}`);
  }
}
