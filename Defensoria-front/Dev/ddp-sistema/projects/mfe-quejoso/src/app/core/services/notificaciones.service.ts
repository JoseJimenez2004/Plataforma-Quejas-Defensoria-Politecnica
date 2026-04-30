import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificacionDTO } from '../../models/notificacion.models';

const BASE = 'http://localhost:8080/api/quejoso/notificaciones';

@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private http = inject(HttpClient);

  listar(): Observable<NotificacionDTO[]> {
    // Sin trailing slash: Spring Boot 3.x no hace redirect de /notificaciones/ a /notificaciones
    return this.http.get<NotificacionDTO[]>(BASE);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.put<void>(`${BASE}/${id}/leer`, null);
  }
}
