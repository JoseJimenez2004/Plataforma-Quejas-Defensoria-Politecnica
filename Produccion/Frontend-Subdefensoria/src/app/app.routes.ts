import { Routes } from '@angular/router';
import { MainLayout } from './layout/main-layout/main-layout';
import { Dashboard } from './features/dashboard/dashboard';
import { Bandeja } from './features/bandeja/bandeja';
import { ExpedienteDetalle } from './features/expediente-detalle/expediente-detalle';
import { Alertas } from './features/alertas/alertas';
import { authSubdefensoriaGuard } from './core/guards/auth-subdefensoria.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    canActivate: [authSubdefensoriaGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: Dashboard },
      { path: 'bandeja', component: Bandeja },
      { path: 'expediente/:folio', component: ExpedienteDetalle },
      { path: 'alertas', component: Alertas }
    ]
  }
];
