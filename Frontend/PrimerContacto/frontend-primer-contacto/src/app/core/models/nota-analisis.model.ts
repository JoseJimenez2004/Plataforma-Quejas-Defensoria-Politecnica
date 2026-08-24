export interface CrearNotaAnalisis {
  folio: string;
  analistaId: number;
  analistaNombre: string;
  contenido: string;
}

export interface NotaAnalisis {
  id: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  contenido: string;

  fechaCreacion: string;
  fechaActualizacion?: string;
}