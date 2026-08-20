import { Routes } from '@angular/router';

import { PublicLayout } from './shared/public-layout/public-layout';
import { Inicio } from './pages/inicio/inicio';
import { RegistroQuejaPublico } from './pages/registro-queja-publico/registro-queja-publico';
import { ConsultarQueja } from './pages/consultar-queja/consultar-queja';
import { CrearCuenta } from './pages/crear-cuenta/crear-cuenta';
import { ActivarCuenta } from './pages/activar-cuenta/activar-cuenta';
import { PortalLogin } from './pages/portal-login/portal-login';
import { RecuperarPassword } from './pages/recuperar-password/recuperar-password';

import { PanelLayout } from './pages/panel/panel-layout/panel-layout';
import { Resumen } from './pages/panel/resumen/resumen';
import { MisQuejas } from './pages/panel/mis-quejas/mis-quejas';
import { QuejaDetalle } from './pages/panel/queja-detalle/queja-detalle';
import { NuevaQueja } from './pages/panel/nueva-queja/nueva-queja';
import { Conciliacion } from './pages/panel/conciliacion/conciliacion';
import { Notificaciones } from './pages/panel/notificaciones/notificaciones';
import { Perfil } from './pages/panel/perfil/perfil';

/** Reconstruido a partir de los routerLink/router.navigate() reales usados en cada página
 * (ver conversación) -- ya que este archivo no existía en el checkout. Dos árboles: el sitio
 * público (bajo PublicLayout: header/footer/chatbot) y el panel autenticado del quejoso (bajo
 * PanelLayout: sidebar propio). No hay guard de ruta en ningún lado del código existente, así
 * que no se agregó ninguno aquí -- mismo comportamiento que ya tenía la app. */
export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Inicio },
      { path: 'queja/registro', component: RegistroQuejaPublico },
      { path: 'queja/consultar', component: ConsultarQueja },
      { path: 'cuenta/crear', component: CrearCuenta },
      { path: 'cuenta/activar', component: ActivarCuenta },
      { path: 'portal/login', component: PortalLogin },
      { path: 'portal/recuperar', component: RecuperarPassword },
    ],
  },
  {
    path: 'panel',
    component: PanelLayout,
    children: [
      { path: '', pathMatch: 'full', component: Resumen },
      { path: 'mis-quejas', component: MisQuejas },
      { path: 'mis-quejas/:folio', component: QuejaDetalle },
      { path: 'nueva-queja', component: NuevaQueja },
      { path: 'conciliacion', component: Conciliacion },
      { path: 'notificaciones', component: Notificaciones },
      { path: 'perfil', component: Perfil },
    ],
  },
  { path: '**', redirectTo: '' },
];
