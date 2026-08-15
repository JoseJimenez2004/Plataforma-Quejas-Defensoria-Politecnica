import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtRevisionInterceptor } from './core/interceptors/jwt-revision.interceptor';
import { changeDetectionInterceptor } from './core/interceptors/change-detection.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtRevisionInterceptor, changeDetectionInterceptor])),
  ],
};
