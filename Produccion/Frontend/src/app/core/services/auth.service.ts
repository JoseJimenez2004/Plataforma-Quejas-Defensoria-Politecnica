import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import {
  ActivacionCuentaRequest,
  AuthResponse,
  LoginRequest,
  ResetPasswordRequest,
  UsuarioActual,
} from '../models/auth.models';

const TOKEN_KEY = 'ddp_token';
const NOMBRE_KEY = 'ddp_nombre';
const CORREO_KEY = 'ddp_correo';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/auth';

  /** Signal reactivo con el usuario actual (o null si no hay sesión). Los
   * componentes pueden leerlo directamente en sus templates: authService.usuarioActual(). */
  readonly usuarioActual = signal<UsuarioActual | null>(this.leerSesionGuardada());

  constructor(private http: HttpClient) {}

  login(datos: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, datos).pipe(
      tap((resp) => this.guardarSesion(resp, datos.correo)),
    );
  }

  solicitarCodigo(correo: string): Observable<string> {
    const params = new URLSearchParams({ correo });
    return this.http.post(`${this.apiUrl}/solicitar-codigo?${params.toString()}`, null, {
      responseType: 'text',
    });
  }

  resetPassword(datos: ResetPasswordRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset-password`, datos, { responseType: 'text' });
  }

  activarCuenta(datos: ActivacionCuentaRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/activar-cuenta`, datos, { responseType: 'text' });
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOMBRE_KEY);
    localStorage.removeItem(CORREO_KEY);
    this.usuarioActual.set(null);
  }

  isLoggedIn(): boolean {
    return !!this.usuarioActual();
  }

  getToken(): string | null {
    return this.usuarioActual()?.token ?? null;
  }

  private guardarSesion(resp: AuthResponse, correo: string): void {
    localStorage.setItem(TOKEN_KEY, resp.token);
    localStorage.setItem(NOMBRE_KEY, resp.nombre);
    localStorage.setItem(CORREO_KEY, correo);
    this.usuarioActual.set({ token: resp.token, nombre: resp.nombre, correo });
  }

  private leerSesionGuardada(): UsuarioActual | null {
    const token = localStorage.getItem(TOKEN_KEY);
    const nombre = localStorage.getItem(NOMBRE_KEY);
    const correo = localStorage.getItem(CORREO_KEY);
    if (!token || !nombre || !correo) {
      return null;
    }
    return { token, nombre, correo };
  }
}
