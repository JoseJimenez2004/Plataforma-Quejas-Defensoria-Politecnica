import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { SesionPersonalService } from '../services/sesion-personal.service';

export const jwtSubdefensoriaInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const sesion = inject(SesionPersonalService);
  const token = sesion.getToken();

  // También firma la consulta a Primer Contacto (pestaña "Primer Contacto" del expediente).
  const esPeticionSubdefensoria =
    req.url.includes('/api/subdefensoria') || req.url.includes('/api/primer-contacto');

  if (!token || !esPeticionSubdefensoria) {
    return next(req);
  }

  const requestAutenticada = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(requestAutenticada);
};