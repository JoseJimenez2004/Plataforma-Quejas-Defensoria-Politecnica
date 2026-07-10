export interface CitaPrimerContacto {
  id?: number;
  quejaId?: number;
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
}

export interface CrearCitaPrimerContacto {
  quejaId: number;
  folio: string;
  quejosoId: number;
  quejosoNombre: string;
  analistaId: number;
  analistaNombre: string;
  fechaCita: string;
  horaCita: string;
  tipoCita: string;
  motivo: string;
}