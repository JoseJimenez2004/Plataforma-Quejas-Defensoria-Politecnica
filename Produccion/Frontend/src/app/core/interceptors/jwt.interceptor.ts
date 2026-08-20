import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';

/** Agrega el JWT guardado (ver AuthService.getToken()) a cada petición saliente hacia el
 * backend, salvo que no haya sesión iniciada. Los endpoints públicos (login, registro,
 * catálogos, consulta de folio, etc.) simplemente ignoran el header si de todas formas llega,
 * así que no hace falta filtrar por URL aquí -- mismo criterio que ya usan
 * JwtAuthenticationFilter en cada microservicio del lado del backend. */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token) {
    return next(req);
  }

  const conAuth = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(conAuth);
};
