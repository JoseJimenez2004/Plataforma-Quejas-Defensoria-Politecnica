import { Routes } from '@angular/router';

import { PublicLayout } from './shared/public-layout/public-layout';
import { Inicio } from './pages/inicio/inicio';

/**
 * Carga diferida (2026-10-14): antes todas las pantallas se importaban aquí arriba y Angular
 * las metía en un solo main.js (~600 KB) que el navegador tenía que bajar completo antes de
 * mostrar cualquier cosa. Ahora solo el layout público y la página de inicio van en el
 * paquete inicial; cada pantalla restante se baja en su propio archivo (chunk-XXXX.js) la
 * primera vez que alguien la abre. El panel del quejoso (sidebar + sus 7 pantallas) también
 * se carga aparte: quien solo registra una queja nunca lo descarga.
 *
 * Dos árboles: el sitio público (bajo PublicLayout: header/footer/chatbot) y el panel
 * autenticado del quejoso (bajo PanelLayout: sidebar propio).
 */

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Inicio },
      {
        path: 'queja/registro',
        loadComponent: () =>
          import('./pages/registro-queja-publico/registro-queja-publico').then((m) => m.RegistroQuejaPublico),
      },
      {
        path: 'queja/consultar',
        loadComponent: () => import('./pages/consultar-queja/consultar-queja').then((m) => m.ConsultarQueja),
      },
      {
        path: 'cuenta/crear',
        loadComponent: () => import('./pages/crear-cuenta/crear-cuenta').then((m) => m.CrearCuenta),
      },
      {
        path: 'cuenta/activar',
        loadComponent: () => import('./pages/activar-cuenta/activar-cuenta').then((m) => m.ActivarCuenta),
      },
      {
        path: 'portal/login',
        loadComponent: () => import('./pages/portal-login/portal-login').then((m) => m.PortalLogin),
      },
      {
        path: 'portal/recuperar',
        loadComponent: () =>
          import('./pages/recuperar-password/recuperar-password').then((m) => m.RecuperarPassword),
      },
      {
        path: 'cita/responder',
        loadComponent: () => import('./pages/responder-cita/responder-cita').then((m) => m.ResponderCita),
      },
      {
        path: 'acuerdo/responder',
        loadComponent: () => import('./pages/responder-acuerdo/responder-acuerdo').then((m) => m.ResponderAcuerdo),
      },
      {
        path: 'denunciado/respuesta',
        loadComponent: () =>
          import('./pages/respuesta-denunciado/respuesta-denunciado').then((m) => m.RespuestaDenunciado),
      },
    ],
  },
  {
    path: 'panel',
    loadComponent: () => import('./pages/panel/panel-layout/panel-layout').then((m) => m.PanelLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./pages/panel/resumen/resumen').then((m) => m.Resumen),
      },
      {
        path: 'mis-quejas',
        loadComponent: () => import('./pages/panel/mis-quejas/mis-quejas').then((m) => m.MisQuejas),
      },
      {
        path: 'mis-quejas/:folio',
        loadComponent: () => import('./pages/panel/queja-detalle/queja-detalle').then((m) => m.QuejaDetalle),
      },
      {
        path: 'nueva-queja',
        loadComponent: () => import('./pages/panel/nueva-queja/nueva-queja').then((m) => m.NuevaQueja),
      },
      {
        path: 'conciliacion',
        loadComponent: () => import('./pages/panel/conciliacion/conciliacion').then((m) => m.Conciliacion),
      },
      {
        path: 'notificaciones',
        loadComponent: () => import('./pages/panel/notificaciones/notificaciones').then((m) => m.Notificaciones),
      },
      {
        path: 'perfil',
        loadComponent: () => import('./pages/panel/perfil/perfil').then((m) => m.Perfil),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
