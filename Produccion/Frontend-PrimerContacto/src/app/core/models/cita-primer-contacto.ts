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

  /** Modalidad (en el backend: tipoCita PRESENCIAL / VIRTUAL). */
  tipo: 'Presencial' | 'Virtual';
  /** Para qué es la cita: COMPARECENCIA, REVISION o SEGUIMIENTO. */
  categoria?: string;
  motivo: string;
  estatus: string;

  fechaCreacion?: string;

  /** Último analista que confirmó, reagendó o canceló. */
  actualizadoPorNombre?: string;

  /** Código del backend: PROGRAMADA, CONFIRMADA, CANCELADA, CANCELADA_QUEJOSO, SIN_RESPUESTA. */
  estatusCodigo?: string;

  /** Respuesta del quejoso (plazo de 48 h al agendar o reagendar). */
  fechaLimiteRespuesta?: string;
  fechaRespuestaQuejoso?: string;
  motivoCancelacionQuejoso?: string;
  /** QUEJOSO si respondió desde su panel; ANALISTA si lo registró el analista. */
  respuestaRegistradaPor?: string;
  /** Si sustituye a una cita reagendada, el id de la anterior. */
  citaAnteriorId?: number;
}

/** Ya no cuentan como cita activa: canceladas por Primer Contacto o sustituidas al reagendar. */
export function citaCerrada(cita: CitaPrimerContacto): boolean {
  return cita.estatusCodigo === 'CANCELADA' || cita.estatusCodigo === 'REAGENDADA';
}

/** Ocupan su horario en la agenda (las demás dejaron libre la hora). */
export function citaOcupaHorario(cita: CitaPrimerContacto): boolean {
  return ['PROGRAMADA', 'CONFIRMADA', 'SIN_RESPUESTA'].includes(cita.estatusCodigo ?? '');
}

export interface ReagendarCitaPrimerContacto {
  fechaCita: string;
  horaCita: string;
  tipoCita?: string;
  categoria?: string;
  motivo?: string;
}

export interface CrearCitaPrimerContacto {
  folio: string;

  quejosoId?: number;
  quejosoNombre?: string;

  fechaCita: string;
  horaCita: string;
  tipoCita: string;
  categoria?: string;
  motivo: string;
}

/** Tipos de cita del diseño 2026-10-18 (columna categoria_cita). */
export const CATEGORIAS_CITA: { clave: string; etiqueta: string }[] = [
  { clave: 'COMPARECENCIA', etiqueta: 'Comparecencia' },
  { clave: 'REVISION', etiqueta: 'Revisión de documentos' },
  { clave: 'SEGUIMIENTO', etiqueta: 'Seguimiento' }
];

export function etiquetaCategoria(clave?: string | null): string {
  return CATEGORIAS_CITA.find(c => c.clave === clave)?.etiqueta ?? 'Comparecencia';
}

/** Horarios de atención que ofrece la agenda (cada 30 minutos). */
export const HORARIOS_CITA = [
  '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30',
  '13:00', '13:30', '14:00', '14:30', '15:00', '15:30', '16:00'
];