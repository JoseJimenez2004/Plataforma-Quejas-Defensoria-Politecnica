import { Routes } from '@angular/router';

import { MainLayout } from './layout/main-layout/main-layout';

import { authPrimerContactoGuard } from './core/guards/auth-primer-contacto.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    canActivate: [authPrimerContactoGuard],

    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)
      },

      {
        path: 'bandeja',
        loadComponent: () => import('./features/bandeja-analisis/bandeja-analisis').then(m => m.BandejaAnalisis)
      },

      {
        path: 'expediente/:id',
        loadComponent: () => import('./features/expediente/expediente').then(m => m.Expediente)
      },

      // Búsqueda de antecedentes de la queja (modelo de búsqueda).
      {
        path: 'expediente/:id/antecedentes',
        loadComponent: () => import('./features/antecedentes/antecedentes').then(m => m.Antecedentes)
      },

      {
        path: 'agenda',
        loadComponent: () => import('./features/agenda/agenda').then(m => m.Agenda)
      },

      {
        path: 'perfil',
        loadComponent: () => import('./features/perfil/perfil').then(m => m.Perfil)
      },

      // El dictamen ahora se registra en la pestaña "Resolución" del expediente (diseño
      // 2026-10-18); la ruta vieja lleva ahí con la decisión ya elegida.
      {
        path: 'dictamen/:id',
        pathMatch: 'full',
        redirectTo: ({ params, queryParams }) =>
          `/expediente/${params['id']}?tab=resolucion&decision=${queryParams['tipo'] === 'improcedente' ? 'improcedente' : 'competente'}`
      },

      // CU-PC-08: consulta del dictamen ya registrado.
      {
        path: 'dictamen/:id/consulta',
        loadComponent: () => import('./features/dictamen-consulta/dictamen-consulta').then(m => m.DictamenConsulta)
      },

      {
        path: 'remision/:id',
        loadComponent: () => import('./features/remision/remision').then(m => m.Remision)
      }
    ]
  }
];