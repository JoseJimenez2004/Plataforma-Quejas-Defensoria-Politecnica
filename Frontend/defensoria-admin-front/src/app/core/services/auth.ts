import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/admin/auth';

  constructor(private http: HttpClient, private router: Router) { }

  login(correo: string, password: string) {
    return this.http.post<any>(`${this.apiUrl}/login`, { correo, password })
      .pipe(
        tap(respuesta => {
          localStorage.setItem('admin_token', respuesta.token);
          localStorage.setItem('admin_nombre', respuesta.nombreCompleto);
          localStorage.setItem('admin_roles', JSON.stringify(respuesta.roles));
        })
      );
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  estaLogueado(): boolean {
    return !!localStorage.getItem('admin_token');
  }
}