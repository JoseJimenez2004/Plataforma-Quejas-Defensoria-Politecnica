import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { SesionPersonalService } from '../services/sesion-personal.service';

export const jwtPrimerContactoInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const sesion = inject(SesionPersonalService);
  const token = sesion.getToken();

  // Solo agregamos el JWT a llamadas de Primer Contacto.
  const esPeticionPrimerContacto =
    req.url.includes('/api/primer-contacto');

  if (!token || !esPeticionPrimerContacto) {
    return next(req);
  }

  const requestAutenticada = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(requestAutenticada);
};