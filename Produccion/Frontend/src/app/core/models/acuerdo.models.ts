/** Acuerdo de conciliación visto desde el enlace que recibe el quejoso — viene de
 * GET /api/subdefensoria/publico/acuerdos/{token}. */
export interface AcuerdoPublico {
  /** Folio de la queja que conoce el quejoso (FOL-...). */
  folioQueja: string;
  quejosoNombre?: string;
  asunto?: string;
  textoAcuerdo: string;
  fechaEnvio?: string;
  /** ACEPTADO | RECHAZADO | null (sin responder). */
  respuestaQuejoso?: 'ACEPTADO' | 'RECHAZADO' | null;
  comentarioQuejoso?: string | null;
  fechaRespuesta?: string | null;
  /** true cuando la Defensoría ya emitió el acuerdo de conclusión. */
  concluido: boolean;
  /** true si todavía puede aceptar o rechazar. */
  puedeResponder: boolean;
}
