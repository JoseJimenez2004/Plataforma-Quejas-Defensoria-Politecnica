export interface Dictamen {
  id?: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  resultado?: string;
  justificacion: string;

  areaTurno?: string;
  responsableTurno?: string;

  fechaDictamen?: string;
  observaciones?: string;
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