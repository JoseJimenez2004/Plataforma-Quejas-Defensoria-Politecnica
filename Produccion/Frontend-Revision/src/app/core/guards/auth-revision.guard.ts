import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthRevisionService } from '../services/auth-revision.service';

export const authRevisionGuard: CanActivateFn = () => {
  const authService = inject(AuthRevisionService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
