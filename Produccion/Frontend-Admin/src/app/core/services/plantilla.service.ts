import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PlantillaDocumento } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class PlantillaService {
  private readonly apiUrl = '/api/admin/plantillas';

  constructor(private http: HttpClient) {}

  listar(): Observable<PlantillaDocumento[]> {
    return this.http.get<PlantillaDocumento[]>(this.apiUrl);
  }

  placeholders(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.apiUrl}/placeholders`);
  }

  obtener(tipo: string): Observable<PlantillaDocumento> {
    return this.http.get<PlantillaDocumento>(`${this.apiUrl}/${tipo}`);
  }

  previsualizar(tipo: string): Observable<string> {
    return this.http.get(`${this.apiUrl}/${tipo}/previsualizar`, { responseType: 'text' });
  }

  actualizar(tipo: string, contenido: string): Observable<PlantillaDocumento> {
    return this.http.put<PlantillaDocumento>(`${this.apiUrl}/${tipo}`, { contenido });
  }
}
