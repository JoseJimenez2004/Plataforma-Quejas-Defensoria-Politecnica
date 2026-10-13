import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthRevisionService } from '../services/auth-revision.service';

import { RUTA_PANEL_DEFENSORA, esRolDefensora } from '../sesion/sesion-personal';

export const authRevisionGuard: CanActivateFn = () => {
  const authService = inject(AuthRevisionService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    const rol = localStorage.getItem('ddp_revision_rol');
    if (esRolDefensora(rol)) {
      window.location.assign(RUTA_PANEL_DEFENSORA);
      return false;
    }
    return true;
  }

  // Sin sesión de revisión, pero con sesión de la Defensora: a su panel, no al login.
  if (esRolDefensora(localStorage.getItem('ddp_admin_rol')) && localStorage.getItem('ddp_admin_token')) {
    window.location.assign(RUTA_PANEL_DEFENSORA);
    return false;
  }

  router.navigate(['/login']);
  return false;
};
