import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ---- Público (con header/footer institucional) ----
  {
    path: '',
    loadComponent: () =>
      import('./shared/public-layout/public-layout').then((m) => m.PublicLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'queja/consultar',
        loadComponent: () =>
          import('./pages/consultar-queja/consultar-queja').then((m) => m.ConsultarQueja),
      },
      {
        path: 'queja/registro',
        loadComponent: () =>
          import('./pages/registro-queja-publico/registro-queja-publico').then(
            (m) => m.RegistroQuejaPublico,
          ),
      },
      {
        path: 'cuenta/crear',
        loadComponent: () =>
          import('./pages/crear-cuenta/crear-cuenta').then((m) => m.CrearCuenta),
      },
      {
        path: 'cuenta/activar',
        loadComponent: () =>
          import('./pages/activar-cuenta/activar-cuenta').then((m) => m.ActivarCuenta),
      },
      {
        path: 'portal/login',
        loadComponent: () =>
          import('./pages/portal-login/portal-login').then((m) => m.PortalLogin),
      },
      {
        path: 'portal/recuperar',
        loadComponent: () =>
          import('./pages/recuperar-password/recuperar-password').then(
            (m) => m.RecuperarPassword,
          ),
      },
    ],
  },

  // ---- Panel autenticado ----
  {
    path: 'panel',
    loadComponent: () =>
      import('./pages/panel/panel-layout/panel-layout').then((m) => m.PanelLayout),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/panel/resumen/resumen').then((m) => m.Resumen),
      },
      {
        path: 'mis-quejas',
        loadComponent: () =>
          import('./pages/panel/mis-quejas/mis-quejas').then((m) => m.MisQuejas),
      },
      {
        path: 'mis-quejas/:folio',
        loadComponent: () =>
          import('./pages/panel/queja-detalle/queja-detalle').then((m) => m.QuejaDetalle),
      },
      {
        path: 'nueva-queja',
        loadComponent: () =>
          import('./pages/panel/nueva-queja/nueva-queja').then((m) => m.NuevaQueja),
      },
      {
        path: 'conciliacion',
        loadComponent: () =>
          import('./pages/panel/conciliacion/conciliacion').then((m) => m.Conciliacion),
      },
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./pages/panel/notificaciones/notificaciones').then((m) => m.Notificaciones),
      },
      {
        path: 'perfil',
        loadComponent: () => import('./pages/panel/perfil/perfil').then((m) => m.Perfil),
      },
    ],
  },

  // ---- Fallback ----
  { path: '**', redirectTo: '' },
];
