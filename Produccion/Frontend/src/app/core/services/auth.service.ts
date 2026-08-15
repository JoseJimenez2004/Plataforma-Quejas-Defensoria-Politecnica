import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import {
  ActivacionCuentaRequest,
  AuthResponse,
  LoginRequest,
  PerfilUpdateRequest,
  PerfilUsuario,
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

  // Antes se pedían con { responseType: 'text' }, pero los errores del backend siempre
  // llegan como JSON ({mensaje,...}) vía GlobalExceptionHandler. Con responseType:'text',
  // Angular también interpreta el cuerpo de un error como texto plano, así que
  // err.error.mensaje quedaba undefined y solo se veía el mensaje genérico de respaldo. Ahora
  // el backend responde JSON tanto en éxito como en error, así que se pide JSON siempre.
  solicitarCodigo(correo: string): Observable<{ mensaje: string }> {
    const params = new URLSearchParams({ correo });
    return this.http.post<{ mensaje: string }>(
      `${this.apiUrl}/solicitar-codigo?${params.toString()}`,
      null,
    );
  }

  resetPassword(datos: ResetPasswordRequest): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/reset-password`, datos);
  }

  activarCuenta(datos: ActivacionCuentaRequest): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/activar-cuenta`, datos);
  }

  /** Perfil completo del usuario logueado (boleta, unidad académica, domicilio, etc.) —
   * el login solo trae nombre/correo, esto completa el resto para "Configuración de Perfil". */
  obtenerPerfil(): Observable<PerfilUsuario> {
    return this.http.get<PerfilUsuario>(`${this.apiUrl}/me`);
  }

  actualizarPerfil(datos: PerfilUpdateRequest): Observable<PerfilUsuario> {
    return this.http.put<PerfilUsuario>(`${this.apiUrl}/perfil`, datos);
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
