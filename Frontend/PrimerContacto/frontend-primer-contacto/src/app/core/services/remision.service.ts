import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Remision, CrearRemisionPayload } from '../models/remision.model';

@Injectable({
  providedIn: 'root'
})
export class RemisionService {

  constructor(private api: ApiService) {}

  crearRemision(dto: CrearRemisionPayload): Observable<Remision> {
    return this.api.post<Remision>('/remisiones', dto);
  }

  obtenerPorQueja(quejaId: number): Observable<Remision> {
    return this.api.get<Remision>(`/remisiones/queja/${quejaId}`);
  }

  obtenerPorFolio(folio: string): Observable<Remision> {
    return this.api.get<Remision>(`/remisiones/folio/${folio}`);
  }

  enviarRemision(quejaId: number): Observable<Remision> {
    return this.api.put<Remision>(`/remisiones/queja/${quejaId}/enviar`, {});
  }
}
