export interface Remision {
  id?: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  autoridadRemision: string;
  justificacionLegal: string;

  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;

  fechaRemision?: string;

  /** GENERADA (oficio listo para descargar) | ENVIADA (se registró el envío). */
  estatus: 'GENERADA' | 'ENVIADA';
  numeroOficio?: string;
  fechaEnvio?: string;
}

export interface CrearRemisionPayload {
  folio: string;

  autoridadRemision: string;
  justificacionLegal: string;

  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;
}
