import { Routes } from '@angular/router';

import { MainLayout } from './layout/main-layout/main-layout';

import { Dashboard } from './features/dashboard/dashboard';
import { BandejaAnalisis } from './features/bandeja-analisis/bandeja-analisis';
import { Expediente } from './features/expediente/expediente';
import { Agenda } from './features/agenda/agenda';
import { Perfil } from './features/perfil/perfil';
import { Remision } from './features/remision/remision';
import { Dictamen } from './features/dictamen/dictamen';
import { DictamenConsulta } from './features/dictamen-consulta/dictamen-consulta';

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
        component: Dashboard
      },

      {
        path: 'bandeja',
        component: BandejaAnalisis
      },

      {
        path: 'expediente/:id',
        component: Expediente
      },

      {
        path: 'agenda',
        component: Agenda
      },

      {
        path: 'perfil',
        component: Perfil
      },

      {
        path: 'dictamen/:id',
        component: Dictamen
      },

      // CU-PC-08: consulta del dictamen ya registrado.
      {
        path: 'dictamen/:id/consulta',
        component: DictamenConsulta
      },

      {
        path: 'remision/:id',
        component: Remision
      }
    ]
  }
];