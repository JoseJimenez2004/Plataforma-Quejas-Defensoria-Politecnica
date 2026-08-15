import { HttpInterceptorFn } from '@angular/common/http';
import { ApplicationRef, inject } from '@angular/core';
import { tap } from 'rxjs';

/**
 * La app corre en modo "zoneless" (Angular 21, sin zone.js instalado) — ver angular.json /
 * package.json, no hay polyfill de zone.js en ningún lado. En ese modo, Angular solo vuelve a
 * revisar la vista automáticamente cuando: se escribe a un signal, ocurre un evento del DOM que
 * el propio Angular esté escuchando (click, submit, change…), o alguien llama a un tick manual.
 *
 * El patrón que se usa en TODO el proyecto (`registro-queja-publico`, `activar-cuenta`,
 * `recuperar-password`, `portal-login`, `nueva-queja`, etc.) actualiza campos normales de la
 * clase dentro del `.subscribe()` de una petición HTTP — ej. `this.cargando = false; this.quejaCreada = r;`.
 * Esa respuesta llega de forma asíncrona, fuera de cualquier evento que Angular esté seguiendo,
 * así que sin este interceptor la vista se queda congelada con el botón en "Enviando…" aunque
 * el dato sí se haya guardado correctamente (por eso los `console`/Network mostraban 200 OK pero
 * la pantalla de éxito nunca aparecía).
 *
 * En vez de reescribir decenas de componentes a signals, se dispara un tick manual después de
 * CADA respuesta u error HTTP — así cualquier página que use el patrón clásico se sigue
 * refrescando sola, sin tener que tocarla una por una.
 */
export const changeDetectionInterceptor: HttpInterceptorFn = (req, next) => {
  const appRef = inject(ApplicationRef);

  // OJO: appRef.tick() NO se puede llamar de forma síncrona aquí, ni siquiera desde un
  // queueMicrotask — Angular en modo zoneless también usa microtasks internamente para
  // programar sus propios ciclos de detección de cambios, así que un queueMicrotask nuestro
  // puede intercalarse con uno de Angular y disparar "NG0101: ApplicationRef.tick is called
  // recursively". Con setTimeout(..., 0) se difiere a un macrotask, que corre después de que
  // se vacíe toda la cola de microtasks (incluida la de Angular), evitando el choque.
  const programarTick = () => {
    setTimeout(() => {
      if (appRef.destroyed) return;
      try {
        appRef.tick();
      } catch (err) {
        // eslint-disable-next-line no-console
        console.error('[changeDetectionInterceptor] appRef.tick() falló:', err);
      }
    }, 0);
  };

  return next(req).pipe(
    tap({
      next: programarTick,
      error: programarTick,
    }),
  );
};
