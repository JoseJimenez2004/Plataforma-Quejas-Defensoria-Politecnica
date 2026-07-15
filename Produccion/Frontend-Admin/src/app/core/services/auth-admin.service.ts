import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { AuthAdminResponse, LoginAdminRequest, RolStaff } from '../models/admin.models';

const TOKEN_KEY = 'ddp_admin_token';
const NOMBRE_KEY = 'ddp_admin_nombre';
const ROL_KEY = 'ddp_admin_rol';
const FORZAR_KEY = 'ddp_admin_forzar_cambio';

export interface UsuarioAdminActual {
  token: string;
  nombre: string;
  rol: RolStaff;
  forzarCambioPassword: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthAdminService {
  private readonly apiUrl = '/api/admin/auth';

  readonly usuarioActual = signal<UsuarioAdminActual | null>(this.leerSesionGuardada());

  constructor(private http: HttpClient) {}

  login(datos: LoginAdminRequest): Observable<AuthAdminResponse> {
    return this.http.post<AuthAdminResponse>(`${this.apiUrl}/login`, datos).pipe(
      tap((resp) => this.guardarSesion(resp)),
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOMBRE_KEY);
    localStorage.removeItem(ROL_KEY);
    localStorage.removeItem(FORZAR_KEY);
    this.usuarioActual.set(null);
  }

  isLoggedIn(): boolean {
    return !!this.usuarioActual();
  }

  getToken(): string | null {
    return this.usuarioActual()?.token ?? null;
  }

  /** Se llama tras un cambio de contraseña exitoso para dejar de pedirlo en cada carga. */
  marcarPasswordActualizada(): void {
    localStorage.setItem(FORZAR_KEY, 'false');
    const actual = this.usuarioActual();
    if (actual) {
      this.usuarioActual.set({ ...actual, forzarCambioPassword: false });
    }
  }

  private guardarSesion(resp: AuthAdminResponse): void {
    localStorage.setItem(TOKEN_KEY, resp.token);
    localStorage.setItem(NOMBRE_KEY, resp.nombre);
    localStorage.setItem(ROL_KEY, resp.rol);
    localStorage.setItem(FORZAR_KEY, String(resp.forzarCambioPassword));
    this.usuarioActual.set({
      token: resp.token,
      nombre: resp.nombre,
      rol: resp.rol,
      forzarCambioPassword: resp.forzarCambioPassword,
    });
  }

  private leerSesionGuardada(): UsuarioAdminActual | null {
    const token = localStorage.getItem(TOKEN_KEY);
    const nombre = localStorage.getItem(NOMBRE_KEY);
    const rol = localStorage.getItem(ROL_KEY) as RolStaff | null;
    const forzarCambioPassword = localStorage.getItem(FORZAR_KEY) === 'true';
    if (!token || !nombre || !rol) {
      return null;
    }
    return { token, nombre, rol, forzarCambioPassword };
  }
}
