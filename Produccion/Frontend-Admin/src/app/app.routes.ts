import { Routes } from '@angular/router';

import { authAdminGuard } from './core/guards/auth-admin.guard';

export const routes: Routes = [
  {
    // Login unificado: /admin/login manda al login único del personal (/revision/login).
    path: 'login',
    canActivate: [() => { window.location.assign('/revision/login'); return false; }],
    children: [],
  },
  {
    path: '',
    loadComponent: () => import('./pages/admin-layout/admin-layout').then((m) => m.AdminLayout),
    canActivate: [authAdminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'usuarios-roles',
        loadComponent: () =>
          import('./pages/usuarios-roles/usuarios-roles').then((m) => m.UsuariosRoles),
      },
      {
        path: 'catalogo-dependencias',
        loadComponent: () =>
          import('./pages/catalogo-dependencias/catalogo-dependencias').then(
            (m) => m.CatalogoDependencias,
          ),
      },
      {
        path: 'plantillas',
        loadComponent: () => import('./pages/plantillas/plantillas').then((m) => m.Plantillas),
      },
      {
        path: 'chatbot',
        loadComponent: () => import('./pages/chatbot/chatbot').then((m) => m.Chatbot),
      },
      {
        path: 'seguridad-respaldos',
        loadComponent: () =>
          import('./pages/seguridad-respaldos/seguridad-respaldos').then(
            (m) => m.SeguridadRespaldos,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
