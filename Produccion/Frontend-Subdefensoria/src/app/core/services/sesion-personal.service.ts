import { Injectable } from '@angular/core';

const TOKEN_KEY = 'ddp_revision_token';
const NOMBRE_KEY = 'ddp_revision_nombre';
const ROL_KEY = 'ddp_revision_rol';
const FORZAR_KEY = 'ddp_revision_forzar_cambio';

/*
 * 2026-11: el Defensor titular (rol DEFENSOR) también entra a Subdefensoría y ve TODOS los
 * expedientes. Él inicia sesión en el panel de la Defensora (/admin/), que guarda la sesión con
 * las claves ddp_admin_*; si no hay sesión de personal y la del panel es de un DEFENSOR, se usa.
 */
const ADMIN_TOKEN_KEY = 'ddp_admin_token';
const ADMIN_NOMBRE_KEY = 'ddp_admin_nombre';
const ADMIN_ROL_KEY = 'ddp_admin_rol';
const ADMIN_FORZAR_KEY = 'ddp_admin_forzar_cambio';

@Injectable({
  providedIn: 'root'
})
export class SesionPersonalService {

  /** true si la sesión viene del panel de la Defensora. */
  private get usaSesionDefensora(): boolean {
    return !localStorage.getItem(TOKEN_KEY) && localStorage.getItem(ADMIN_ROL_KEY) === 'DEFENSOR'
      && !!localStorage.getItem(ADMIN_TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.usaSesionDefensora ? ADMIN_TOKEN_KEY : TOKEN_KEY);
  }

  getNombre(): string | null {
    return localStorage.getItem(this.usaSesionDefensora ? ADMIN_NOMBRE_KEY : NOMBRE_KEY);
  }

  getRol(): string | null {
    return localStorage.getItem(this.usaSesionDefensora ? ADMIN_ROL_KEY : ROL_KEY);
  }

  debeCambiarPassword(): boolean {
    return localStorage.getItem(this.usaSesionDefensora ? ADMIN_FORZAR_KEY : FORZAR_KEY) === 'true';
  }

  haySesion(): boolean {
    return !!this.getToken();
  }

  /** Puede usar Subdefensoría: abogado (SUBDEFENSOR) o el Defensor titular (DEFENSOR). */
  esSubdefensor(): boolean {
    const rol = this.getRol();
    return rol === 'SUBDEFENSOR' || rol === 'DEFENSOR';
  }

  /** El titular supervisa: ve los expedientes de todos los abogados. */
  esDefensorTitular(): boolean {
    return this.getRol() === 'DEFENSOR';
  }

  cerrarSesion(): void {
    if (this.usaSesionDefensora) {
      localStorage.removeItem(ADMIN_TOKEN_KEY);
      localStorage.removeItem(ADMIN_NOMBRE_KEY);
      localStorage.removeItem(ADMIN_ROL_KEY);
      localStorage.removeItem(ADMIN_FORZAR_KEY);
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOMBRE_KEY);
    localStorage.removeItem(ROL_KEY);
    localStorage.removeItem(FORZAR_KEY);
  }
}
