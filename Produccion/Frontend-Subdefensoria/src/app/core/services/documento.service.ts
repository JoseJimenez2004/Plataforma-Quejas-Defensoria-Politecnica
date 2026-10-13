import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';

/** PDF de Subdefensoría (formato de ejemplo): oficio, recordatorio y acuerdos. */
@Injectable({ providedIn: 'root' })
export class DocumentoService {
  constructor(private api: ApiService) {}

  oficio(oficioId: number): Observable<Blob> {
    return this.api.getBlob(`/documentos/oficio/${oficioId}`);
  }

  recordatorio(recordatorioId: number): Observable<Blob> {
    return this.api.getBlob(`/documentos/recordatorio/${recordatorioId}`);
  }

  acuerdoConciliacion(expedienteId: number): Observable<Blob> {
    return this.api.getBlob(`/documentos/acuerdo-conciliacion/${expedienteId}`);
  }

  acuerdoConclusion(expedienteId: number): Observable<Blob> {
    return this.api.getBlob(`/documentos/acuerdo-conclusion/${expedienteId}`);
  }
}

/** Abre un PDF recibido como Blob en una pestaña nueva. */
export function abrirBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
  // Se libera después: la pestaña nueva ya cargó el contenido para entonces.
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
