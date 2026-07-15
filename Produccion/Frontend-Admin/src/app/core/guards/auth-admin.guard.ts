import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthAdminService } from '../services/auth-admin.service';

export const authAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthAdminService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
