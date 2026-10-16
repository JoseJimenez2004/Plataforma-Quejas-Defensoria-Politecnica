/** Cita de Primer Contacto vista desde el enlace que recibe el quejoso — viene de
 * GET /api/primer-contacto/publico/citas/{token}. */
export interface CitaPublica {
  /** Folio de la queja que conoce el quejoso (FOL-...). */
  folioQueja: string;
  quejosoNombre?: string;
  /** "2026-10-15" */
  fechaCita: string;
  /** "10:30" o "10:30:00" */
  horaCita: string;
  /** "PRESENCIAL" | "VIRTUAL" */
  tipoCita: string;
  motivo?: string;
  /** "PROGRAMADA" | "CONFIRMADA" | "CANCELADA_QUEJOSO" | "SIN_RESPUESTA" | "REAGENDADA" | "CANCELADA" */
  estatus: string;
  fechaLimiteRespuesta?: string;
  motivoCancelacionQuejoso?: string;
  analistaNombre?: string;
  /** true si todavía puede confirmar o cancelar (pendiente y dentro del plazo). */
  puedeResponder: boolean;
}
