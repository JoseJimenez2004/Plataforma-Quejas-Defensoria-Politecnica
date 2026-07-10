export interface CrearNotaAnalisis {
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  contenido: string;
}

export interface NotaAnalisis {
  id: number;
  quejaId: number;
  folio: string;
  analistaId: number;
  analistaNombre: string;
  contenido: string;
  fechaCreacion: string;
  fechaActualizacion?: string;
}