/** Un aviso del centro de notificaciones del quejoso (login, cambios de estatus, nuevos
 * acuerdos de conciliación) — viene de GET /api/notificaciones/mias. */
export interface Notificacion {
  id: number;
  correoDestino: string;
  /** "LOGIN" | "CAMBIO_ESTATUS" | "CONCILIACION" */
  tipo: string;
  titulo: string;
  mensaje: string;
  leida: boolean;
  fechaCreacion: string;
  enlace?: string;
}
