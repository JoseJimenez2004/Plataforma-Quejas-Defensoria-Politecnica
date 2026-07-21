export interface Dictamen {
  id?: number;
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  resultado?: string;
  justificacion: string;
  areaTurno?: string;
  responsableTurno?: string;
  fechaDictamen?: string;
}

export interface CompetenciaPayload {
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  justificacion: string;
  areaTurno: string;
  responsableTurno: string;
  observaciones?: string;
}

export interface ImprocedenciaPayload {
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  justificacion: string;
}
