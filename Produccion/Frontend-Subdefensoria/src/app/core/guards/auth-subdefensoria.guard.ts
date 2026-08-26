import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { SesionPersonalService } from '../services/sesion-personal.service';

export const authSubdefensoriaGuard: CanActivateFn = () => {

  const sesion = inject(SesionPersonalService);

  if (
    sesion.haySesion() &&
    sesion.esSubdefensor()
  ) {
    return true;
  }

  // El login está en el frontend de Revisión.
  window.location.assign('/revision/login');

  return false;
};