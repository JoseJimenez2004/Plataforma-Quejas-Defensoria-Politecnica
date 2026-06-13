import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { QuejaSeguimientoDTO, QuejaFiltroDTO } from '../../models/queja.models';
import { TramitesResumenDTO } from '../../models/perfil.models';

const BASE = 'http://localhost:8080/api/quejoso/tramites';

@Injectable({ providedIn: 'root' })
export class TramitesService {
  private http = inject(HttpClient);

  resumen(): Observable<TramitesResumenDTO> {
    return this.http.get<TramitesResumenDTO>(`${BASE}/resumen`);
  }

  historial(filtro: QuejaFiltroDTO): Observable<QuejaSeguimientoDTO[]> {
    let params = new HttpParams();
    if (filtro.folio) params = params.set('folio', filtro.folio);
    if (filtro.estatus) params = params.set('estatus', filtro.estatus);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);
    return this.http.get<QuejaSeguimientoDTO[]>(`${BASE}/historial`, { params });
  }
}
