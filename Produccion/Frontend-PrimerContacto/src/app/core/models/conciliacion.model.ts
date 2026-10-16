/** Acuerdo de conciliación (tabla compartida acuerdos_conciliacion, CU-PC-10). */
export interface AcuerdoConciliacion {
  id: number;

  /** Folio de la queja (FOL-...), el que ve el quejoso. */
  numeroFolio: string;

  asunto: string;
  terminos: string;

  estado: 'PENDIENTE' | 'ACEPTADO' | 'RECHAZADO';

  fechaEmision?: string;
  fechaRespuesta?: string;
  comentarioQuejoso?: string;

  creadoPor?: string;
  creadoPorNombre?: string;
}

export interface CrearConciliacionPayload {
  /** Folio de Primer Contacto (PC-...). */
  folio: string;
  asunto: string;
  terminos: string;
}
