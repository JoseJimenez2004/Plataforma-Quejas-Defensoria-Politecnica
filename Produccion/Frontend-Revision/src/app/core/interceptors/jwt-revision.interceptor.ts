import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthRevisionService } from '../services/auth-revision.service';

/** Adjunta el JWT a toda petición hacia /api/... Si el backend responde 401, cierra la
 * sesión local y manda de vuelta al login (token vencido/rol sin permiso). */
export const jwtRevisionInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthRevisionService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token && req.url.startsWith('/api/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err) => {
      if (err.status === 401 && req.url.startsWith('/api/')) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
