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

  /** Último analista que confirmó, reagendó o canceló. */
  actualizadoPorNombre?: string;
}

export interface ReagendarCitaPrimerContacto {
  fechaCita: string;
  horaCita: string;
  tipoCita?: string;
  motivo?: string;
}

export interface CrearCitaPrimerContacto {
  folio: string;

  quejosoId?: number;
  quejosoNombre?: string;

  fechaCita: string;
  horaCita: string;
  tipoCita: string;
  motivo: string;
}