/*
 * La nota de análisis es el OFICIO del expediente: asunto + texto, con su PDF
 * (GET /notas/{id}/pdf). En el backend conserva el nombre "nota".
 */
export interface CrearNotaAnalisis {
  folio: string;
  asunto: string;
  contenido: string;
}

export interface NotaAnalisis {
  id: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  /** DDP/PC/OF/año/id */
  numeroOficio: string;
  /** null en las notas creadas antes de los oficios. */
  asunto?: string | null;
  contenido: string;

  fechaCreacion: string;
  fechaActualizacion?: string;
}