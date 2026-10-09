import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthAdminService } from '../services/auth-admin.service';
import { irALoginPersonal } from '../sesion/sesion-personal';

/** Adjunta el JWT del panel a toda petición hacia /api/... Si el backend responde 401/403,
 * cierra la sesión local y manda de vuelta al login (token vencido/rol sin permiso). */
export const jwtAdminInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthAdminService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token && req.url.startsWith('/api/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err) => {
      // La campana de notificaciones nunca debe sacar al personal del panel.
      if (err.status === 401 && req.url.startsWith('/api/') && !req.url.startsWith('/api/notificaciones/')) {
        authService.logout();
        irALoginPersonal();
      }
      return throwError(() => err);
    }),
  );
};
