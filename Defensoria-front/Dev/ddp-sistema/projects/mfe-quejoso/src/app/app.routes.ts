import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    loadComponent: () =>
      import('./features/auth/registro/registro.component').then((m) => m.RegistroComponent),
  },
  {
    path: 'queja/nueva',
    loadComponent: () =>
      import('./features/queja/formulario/formulario-queja.component').then(
        (m) => m.FormularioQuejaComponent
      ),
  },
  {
    path: 'queja/activar',
    loadComponent: () =>
      import('./features/auth/activar-folio/activar-folio.component').then(
        (m) => m.ActivarFolioComponent
      ),
  },
  {
    path: 'queja/configurar-cuenta',
    loadComponent: () =>
      import('./features/auth/configurar-cuenta/configurar-cuenta.component').then(
        (m) => m.ConfigurarCuentaComponent
      ),
  },
  {
    path: 'queja/exito',
    loadComponent: () =>
      import('./features/queja/exito/pantalla-exito.component').then(
        (m) => m.PantallaExitoComponent
      ),
  },
  {
    path: 'restablecer-password',
    loadComponent: () =>
      import('./features/auth/restablecer-password/restablecer-password.component').then(
        (m) => m.RestablecerPasswordComponent
      ),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/layout/dashboard-layout.component').then(
        (m) => m.DashboardLayoutComponent
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/gestion-tramites/gestion-tramites.component').then(
            (m) => m.GestionTramitesComponent
          ),
      },
      {
        path: 'historial',
        loadComponent: () =>
          import('./features/dashboard/historial/historial.component').then(
            (m) => m.HistorialComponent
          ),
      },
      {
        path: 'nueva',
        loadComponent: () =>
          import('./features/dashboard/nueva-queja/nueva-queja.component').then(
            (m) => m.NuevaQuejaComponent
          ),
      },
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./features/dashboard/notificaciones/notificaciones.component').then(
            (m) => m.NotificacionesComponent
          ),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/dashboard/perfil/perfil.component').then((m) => m.PerfilComponent),
      },
      {
        path: 'acuerdos',
        loadComponent: () =>
          import('./features/dashboard/acuerdos/acuerdos.component').then(
            (m) => m.AcuerdosComponent
          ),
      },
      {
        path: 'acuerdos/:folio',
        loadComponent: () =>
          import('./features/dashboard/conciliacion/conciliacion.component').then(
            (m) => m.ConciliacionComponent
          ),
      },
      {
        path: 'queja/:folio',
        loadComponent: () =>
          import('./features/dashboard/detalle-queja/detalle-queja.component').then(
            (m) => m.DetalleQuejaComponent
          ),
      },
      {
        path: 'queja/:folio/editar',
        loadComponent: () =>
          import('./features/dashboard/edicion-queja/edicion-queja.component').then(
            (m) => m.EdicionQuejaComponent
          ),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
