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
}

export interface CrearRemisionPayload {
  folio: string;

  analistaId: number;
  analistaNombre: string;

  autoridadRemision: string;
  justificacionLegal: string;

  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;
}