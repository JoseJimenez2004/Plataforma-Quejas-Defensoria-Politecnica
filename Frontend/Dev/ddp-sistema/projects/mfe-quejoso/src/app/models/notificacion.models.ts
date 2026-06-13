export type TipoNotificacion = 'ESTATUS' | 'REQUERIMIENTO' | 'CONCILIACION';

export interface NotificacionDTO {
  id: number;
  titulo: string;
  mensaje: string;
  fecha: string;
  tipo: TipoNotificacion;
  leida: boolean;
  folioRelacionado: string;
}
