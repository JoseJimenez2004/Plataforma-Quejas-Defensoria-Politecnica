import { Routes } from '@angular/router';
import { MainLayout } from './layout/main-layout/main-layout';
import { Dashboard } from './features/dashboard/dashboard';
import { BandejaAnalisis } from './features/bandeja-analisis/bandeja-analisis';
import { Expediente } from './features/expediente/expediente';
import { Agenda } from './features/agenda/agenda';
import { Perfil } from './features/perfil/perfil';
import { Remision } from './features/remision/remision';
import { Dictamen } from './features/dictamen/dictamen';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: Dashboard },
      { path: 'bandeja', component: BandejaAnalisis },
      { path: 'expediente/:id', component: Expediente },
      { path: 'agenda', component: Agenda },
      { path: 'perfil', component: Perfil },
      { path: 'dictamen/:id', component: Dictamen },
    { path: 'remision/:id', component: Remision }
    ]
  }
];