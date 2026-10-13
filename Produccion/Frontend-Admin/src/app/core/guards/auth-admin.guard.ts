import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthAdminService } from '../services/auth-admin.service';

/** Login unificado: este panel ya no tiene login propio, todo el personal entra por
 * /revision/login y ahí se le manda aquí si su rol es DEFENSOR o ADMIN_SISTEMAS. */
import { esRolDefensora, irALoginPersonal } from '../sesion/sesion-personal';

export const authAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthAdminService);

  if (authService.isLoggedIn() && esRolDefensora(authService.usuarioActual()?.rol)) {
    return true;
  }

  // Sin sesión, o con una sesión de otro rol (p. ej. recepcionista): al login único.
  authService.logout();
  irALoginPersonal();
  return false;
};
