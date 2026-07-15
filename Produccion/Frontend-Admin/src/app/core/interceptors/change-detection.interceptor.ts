import { HttpInterceptorFn } from '@angular/common/http';
import { ApplicationRef, inject } from '@angular/core';
import { tap } from 'rxjs';

/** Misma app zoneless que el sitio de quejosos: sin esto, una respuesta HTTP resuelta dentro
 * de un subscribe() no siempre refresca la vista sola. Se dispara con setTimeout (no
 * síncrono) para no chocar con el ciclo de detección de cambios que ya esté en curso. */
export const changeDetectionInterceptor: HttpInterceptorFn = (req, next) => {
  const appRef = inject(ApplicationRef);
  return next(req).pipe(
    tap(() => {
      setTimeout(() => {
        try {
          appRef.tick();
        } catch {
          // Si ya hay un tick en curso, se ignora -- el siguiente ciclo natural lo cubre.
        }
      }, 0);
    }),
  );
};
