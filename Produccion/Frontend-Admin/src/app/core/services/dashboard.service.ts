import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DashboardResumen } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = '/api/admin/dashboard';

  constructor(private http: HttpClient) {}

  resumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.apiUrl}/resumen`);
  }
}
