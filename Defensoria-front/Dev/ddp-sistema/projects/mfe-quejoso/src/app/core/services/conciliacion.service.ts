import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConciliacionDTO } from '../../models/perfil.models';

const BASE = 'http://localhost:8080/api/quejoso/conciliacion';

@Injectable({ providedIn: 'root' })
export class ConciliacionService {
  private http = inject(HttpClient);

  obtener(folio: string): Observable<ConciliacionDTO> {
    return this.http.get<ConciliacionDTO>(`${BASE}/${folio}`);
  }
}
