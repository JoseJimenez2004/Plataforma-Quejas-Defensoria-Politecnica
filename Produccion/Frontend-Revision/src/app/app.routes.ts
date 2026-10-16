import { Routes } from '@angular/router';

import { authRevisionGuard } from './core/guards/auth-revision.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    loadComponent: () => import('./pages/recepcion-layout/recepcion-layout').then((m) => m.RecepcionLayout),
    canActivate: [authRevisionGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/bandeja/bandeja').then((m) => m.Bandeja),
      },
      {
        path: 'validacion/:folio',
        loadComponent: () => import('./pages/validacion/validacion').then((m) => m.Validacion),
      },
      {
        path: 'rechazo/:folio',
        loadComponent: () => import('./pages/rechazo/rechazo').then((m) => m.Rechazo),
      },
      {
        path: 'turnado/:folio',
        loadComponent: () => import('./pages/turnado/turnado').then((m) => m.Turnado),
      },
      {
        path: 'registro-manual',
        // Registro manual = el mismo formulario del portal público + datos de ventanilla (2026-10-16).
        loadComponent: () => import('./pages/registro-ventanilla/registro-ventanilla').then((m) => m.RegistroVentanilla),
      },
      {
        path: 'historial',
        loadComponent: () => import('./pages/historial/historial').then((m) => m.Historial),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
