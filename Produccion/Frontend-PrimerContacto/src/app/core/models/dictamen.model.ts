export interface Dictamen {
  id?: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  /** COMPETENTE | IMPROCEDENTE */
  resultado?: string;
  justificacion: string;

  areaTurno?: string;
  responsableTurno?: string;

  fechaDictamen?: string;
  observaciones?: string;

  /** En qué quedó el expediente y, si fue procedente, si Subdefensoría ya lo recibió. */
  estatusExpediente?: string;
  folioSubdefensoria?: string;
}

export interface CompetenciaPayload {
  folio: string;

  justificacion: string;

  areaTurno: string;
  responsableTurno: string;

  observaciones?: string;
}

export interface ImprocedenciaPayload {
  folio: string;

  justificacion: string;
}
