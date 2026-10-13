/**
 * Login unificado del personal (2026-10-08). Todo el personal entra por /revision/login; según
 * el rol, cada quien termina en su panel:
 *   RECEPCIONISTA                -> /revision/  (este frontend)
 *   DEFENSOR / ADMIN_SISTEMAS    -> /admin/     (Frontend-Admin, "panel de la Defensora")
 *   ANALISTA_PRIMER_CONTACTO     -> /primer-contacto/
 *   SUBDEFENSOR                  -> /subdefensoria/
 *
 * Los dos frontends viven en el mismo dominio, así que comparten localStorage: el login deja
 * la sesión con las claves que cada panel ya lee (ddp_revision_* o ddp_admin_*).
 * IMPORTANTE: este archivo existe igual en Frontend-Revision y en Frontend-Admin; si cambias
 * uno, cambia el otro.
 */
export const RUTA_LOGIN_PERSONAL = '/revision/login';
export const RUTA_PANEL_DEFENSORA = '/admin/';
export const ROLES_DEFENSORA = ['DEFENSOR', 'ADMIN_SISTEMAS'];

const CLAVES_ADMIN = ['ddp_admin_token', 'ddp_admin_nombre', 'ddp_admin_rol', 'ddp_admin_forzar_cambio'];
const CLAVES_REVISION = ['ddp_revision_token', 'ddp_revision_nombre', 'ddp_revision_rol', 'ddp_revision_forzar_cambio'];

export interface SesionPersonal {
  token: string;
  nombre: string;
  rol: string;
  forzarCambioPassword: boolean;
}

export function esRolDefensora(rol: string | null | undefined): boolean {
  return !!rol && ROLES_DEFENSORA.includes(rol);
}

/** Deja la sesión donde la lee el panel de la Defensora (Frontend-Admin). */
export function guardarSesionDefensora(sesion: SesionPersonal): void {
  localStorage.setItem('ddp_admin_token', sesion.token);
  localStorage.setItem('ddp_admin_nombre', sesion.nombre);
  localStorage.setItem('ddp_admin_rol', sesion.rol);
  localStorage.setItem('ddp_admin_forzar_cambio', String(sesion.forzarCambioPassword));
}

/** Cierra la sesión del personal en TODOS los paneles (un solo login, una sola salida). */
export function cerrarSesionPersonal(): void {
  [...CLAVES_ADMIN, ...CLAVES_REVISION].forEach((clave) => localStorage.removeItem(clave));
}

export function irALoginPersonal(): void {
  window.location.assign(RUTA_LOGIN_PERSONAL);
}
