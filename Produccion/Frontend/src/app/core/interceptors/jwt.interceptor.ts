import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/** Adjunta el JWT a toda petición que vaya hacia nuestra propia API (/api/...),
 * para que las rutas protegidas de queja-service/auth-service lo reciban. Si el
 * backend responde 401/403 (token vencido o inválido), cierra la sesión local y
 * manda de vuelta al login en vez de dejar al usuario en un error genérico. */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token && req.url.startsWith('/api/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err) => {
      if ((err.status === 401 || err.status === 403) && req.url.startsWith('/api/')) {
        authService.logout();
        router.navigate(['/portal/login']);
      }
      return throwError(() => err);
    }),
  );
};
