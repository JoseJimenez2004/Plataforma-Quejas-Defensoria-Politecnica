import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { changeDetectionInterceptor } from './core/interceptors/change-detection.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // changeDetectionInterceptor va al final para que corra sobre la respuesta ya resuelta
    // (después de que jwtInterceptor haya hecho su parte). Ver el comentario en ese archivo:
    // sin esto, ninguna pantalla que actualice campos dentro de un subscribe() de HttpClient
    // refresca la vista (la app corre zoneless, sin zone.js).
    provideHttpClient(withInterceptors([jwtInterceptor, changeDetectionInterceptor])),
  ],
};
