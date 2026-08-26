import { Injectable } from '@angular/core';

const TOKEN_KEY = 'ddp_revision_token';
const NOMBRE_KEY = 'ddp_revision_nombre';
const ROL_KEY = 'ddp_revision_rol';
const FORZAR_KEY = 'ddp_revision_forzar_cambio';

@Injectable({
  providedIn: 'root'
})
export class SesionPersonalService {

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getNombre(): string | null {
    return localStorage.getItem(NOMBRE_KEY);
  }

  getRol(): string | null {
    return localStorage.getItem(ROL_KEY);
  }

  debeCambiarPassword(): boolean {
    return localStorage.getItem(FORZAR_KEY) === 'true';
  }

  haySesion(): boolean {
    return !!this.getToken();
  }

  esSubdefensor(): boolean {
    return this.getRol() === 'SUBDEFENSOR';
  }

  cerrarSesion(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOMBRE_KEY);
    localStorage.removeItem(ROL_KEY);
    localStorage.removeItem(FORZAR_KEY);
  }
}