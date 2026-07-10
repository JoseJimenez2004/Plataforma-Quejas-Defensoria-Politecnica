export interface Remision {
  id?: number;
  quejaId: number;
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
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  autoridadRemision: string;
  justificacionLegal: string;
  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;
}
