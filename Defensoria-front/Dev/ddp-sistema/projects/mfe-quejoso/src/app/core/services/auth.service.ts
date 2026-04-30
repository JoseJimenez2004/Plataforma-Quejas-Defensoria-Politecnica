import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import {
  LoginDTO,
  ActivacionCuentaDTO,
  ResetPasswordDTO,
  AuthRespuestaDTO,
  UsuarioPerfilDTO,
} from '../../models/auth.models';

const BASE = 'http://localhost:8080/api/quejoso/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  login(dto: LoginDTO): Observable<AuthRespuestaDTO> {
    return this.http.post<AuthRespuestaDTO>(`${BASE}/login`, dto).pipe(
      tap((resp) => {
        localStorage.setItem('ddp_token', resp.token);
        localStorage.setItem('ddp_perfil', JSON.stringify(resp.perfil));
      })
    );
  }

  activarCuenta(dto: ActivacionCuentaDTO): Observable<string> {
    return this.http.post(`${BASE}/activar-cuenta`, dto, { responseType: 'text' });
  }

  solicitarCodigo(correo: string): Observable<string> {
    return this.http.post(`${BASE}/solicitar-codigo`, null, {
      params: { correo },
      responseType: 'text',
    });
  }

  resetPassword(dto: ResetPasswordDTO): Observable<string> {
    return this.http.post(`${BASE}/reset-password`, dto, { responseType: 'text' });
  }

  logout(): void {
    localStorage.removeItem('ddp_token');
    localStorage.removeItem('ddp_perfil');
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('ddp_token');
  }

  getPerfil(): UsuarioPerfilDTO | null {
    const raw = localStorage.getItem('ddp_perfil');
    return raw ? (JSON.parse(raw) as UsuarioPerfilDTO) : null;
  }
}
