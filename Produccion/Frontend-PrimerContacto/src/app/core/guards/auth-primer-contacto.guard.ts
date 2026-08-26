import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { SesionPersonalService } from '../services/sesion-personal.service';

export const authPrimerContactoGuard: CanActivateFn = () => {

  const sesion = inject(SesionPersonalService);

  if (
    sesion.haySesion() &&
    sesion.esAnalistaPrimerContacto()
  ) {
    return true;
  }

  /*
   * El login pertenece al frontend de Revisión/Recepción.
   *
   * No usamos Router porque es otra aplicación Angular.
   */
  window.location.assign('/revision/login');

  return false;
};