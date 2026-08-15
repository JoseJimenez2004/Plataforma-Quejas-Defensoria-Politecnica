/** Un acuerdo de conciliación emitido por el personal de la Defensoría para una queja
 * propia — viene de GET /api/quejoso/conciliaciones/mias (queja-service). */
export interface AcuerdoConciliacion {
  id: number;
  numeroFolio: string;
  correoInstitucional: string;
  asunto: string;
  terminos: string;
  /** "PENDIENTE" | "ACEPTADO" | "RECHAZADO" */
  estado: string;
  fechaEmision: string;
  fechaRespuesta?: string;
  comentarioQuejoso?: string;
  creadoPor?: string;
}

/** Body de PUT /api/quejoso/conciliaciones/{id}/respuesta. */
export interface RespuestaConciliacionRequest {
  estado: 'ACEPTADO' | 'RECHAZADO';
  comentario?: string;
}
