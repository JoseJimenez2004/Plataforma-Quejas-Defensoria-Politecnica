import { HttpErrorResponse } from '@angular/common/http';

/** Abre un archivo recibido como Blob en una pestaña nueva (evidencias). */
export function abrirBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  // Se libera después: la pestaña nueva ya cargó el contenido para entonces.
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Descarga un Blob con el nombre indicado (oficio PDF). */
export function descargarBlob(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

/**
 * Mensaje de error del backend ({"error": "..."}, ver GlobalExceptionHandler) o uno
 * genérico. Los 409 traen explicaciones pensadas para mostrarse tal cual al analista.
 */
export function mensajeDeError(error: unknown, porDefecto: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const mensaje = (error.error as { error?: string }).error;
    if (mensaje && error.status < 500) {
      return mensaje;
    }
  }
  return porDefecto;
}
