import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Dependencia } from '../models/admin.models';

export interface DependenciaRequest {
  clave: string;
  clavePadre?: string;
  nombre: string;
  abreviatura?: string;
  tipo: string;
  categoria?: string;
  nivel?: number;
  correoContacto?: string;
  nombreTitular?: string;
}

export interface ImportacionResumen {
  filasCreadas: number;
  filasActualizadas: number;
  errores: string[];
}

@Injectable({ providedIn: 'root' })
export class CatalogoAdminService {
  private readonly apiUrl = '/api/catalogos/dependencias/admin';

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<Dependencia[]> {
    return this.http.get<Dependencia[]>(this.apiUrl);
  }

  crear(datos: DependenciaRequest): Observable<Dependencia> {
    return this.http.post<Dependencia>(this.apiUrl, datos);
  }

  editar(clave: string, datos: DependenciaRequest): Observable<Dependencia> {
    return this.http.put<Dependencia>(`${this.apiUrl}/${encodeURIComponent(clave)}`, datos);
  }

  importarExcel(archivo: File): Observable<ImportacionResumen> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<ImportacionResumen>(`${this.apiUrl}/importar-excel`, formData);
  }
}
