import { Injectable } from '@angular/core';

const TOKEN_KEY = 'ddp_revision_token';
const NOMBRE_KEY = 'ddp_revision_nombre';
const ROL_KEY = 'ddp_revision_rol';
const FORZAR_KEY = 'ddp_revision_forzar_cambio';

/*
 * 2026-11-17: la Defensora (rol DEFENSOR) ya no entra a Subdefensoría; consulta el detalle de las
 * quejas desde "Quejas" en su panel (/admin/). Aquí solo entran los abogados (SUBDEFENSOR).
 */

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

  /** Puede usar Subdefensoría: solo el abogado (SUBDEFENSOR). */
  esSubdefensor(): boolean {
    return this.getRol() === 'SUBDEFENSOR';
  }

  /**
   * Modo "solo consulta" del titular. Desde 2026-11-17 el guard ya no deja entrar al DEFENSOR,
   * así que en la práctica siempre es false; se conserva para no tocar las pantallas.
   */
  esDefensorTitular(): boolean {
    return this.getRol() === 'DEFENSOR';
  }

  cerrarSesion(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOMBRE_KEY);
    localStorage.removeItem(ROL_KEY);
    localStorage.removeItem(FORZAR_KEY);
  }
}
