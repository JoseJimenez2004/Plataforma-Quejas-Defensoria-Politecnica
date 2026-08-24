export interface CitaPrimerContacto {
  id?: number;
  expedienteId?: number;
  folio: string;

  quejosoId?: number;
  quejoso: string;

  analistaId?: number;
  analistaNombre?: string;

  fecha: string;
  hora: string;

  tipo: 'Presencial' | 'Virtual';
  motivo: string;
  estatus: string;

  fechaCreacion?: string;
}

export interface CrearCitaPrimerContacto {
  folio: string;

  quejosoId?: number;
  quejosoNombre?: string;

  analistaId: number;
  analistaNombre: string;

  fechaCita: string;
  horaCita: string;
  tipoCita: string;
  motivo: string;
}