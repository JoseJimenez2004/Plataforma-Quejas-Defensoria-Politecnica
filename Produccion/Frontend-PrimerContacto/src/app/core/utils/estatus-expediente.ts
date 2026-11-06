/**
 * Estados de la queja mientras está en Primer Contacto: los mismos códigos que el backend
 * (EstatusExpediente.java) y que el diagrama de estados.
 *
 *   TURNADA -> EN_ANALISIS -> PROCEDENTE           (pasa a Subdefensoría)
 *                          -> IMPROCEDENTE -> REMITIDA
 *
 * Antes estas etiquetas estaban copiadas en bandeja.service y expediente.service, con
 * ramas para estados que el backend nunca asignaba (COMPETENTE, CON_CITA).
 */
export type CodigoEstatusExpediente =
  | 'TURNADA'
  | 'EN_ANALISIS'
  | 'PROCEDENTE'
  | 'IMPROCEDENTE'
  | 'REMITIDA';

const ETIQUETAS: Record<string, string> = {
  TURNADA: 'Turnada',
  EN_ANALISIS: 'En análisis',
  PROCEDENTE: 'Procedente',
  IMPROCEDENTE: 'Improcedente',
  REMITIDA: 'Remitida',
  // Estado viejo: la remisión redactada ahora es un estatus de la remisión, no del expediente.
  PENDIENTE_REMISION: 'Improcedente'
};

export function etiquetaEstatus(codigo: string | null | undefined): string {
  if (!codigo) return '';
  return ETIQUETAS[codigo.toUpperCase()] ?? codigo;
}

/** Normaliza el código (incluye el estado viejo PENDIENTE_REMISION). */
export function codigoEstatus(codigo: string | null | undefined): CodigoEstatusExpediente | '' {
  const valor = (codigo ?? '').toUpperCase();
  if (valor === 'PENDIENTE_REMISION') return 'IMPROCEDENTE';
  return valor as CodigoEstatusExpediente;
}

/** Mientras está abierto se pueden agendar citas, conciliar y dictaminar. */
export function estaAbierto(codigo: string | null | undefined): boolean {
  const valor = codigoEstatus(codigo);
  return valor === 'TURNADA' || valor === 'EN_ANALISIS';
}

/** Clase CSS del chip de estatus (colores en styles.css: .estatus-*). */
export function claseEstatus(codigo: string | null | undefined): string {
  return 'estatus-' + (codigoEstatus(codigo) || 'desconocido').toLowerCase().replace('_', '-');
}

/**
 * PC-1 (2026-11): la prioridad la asigna el analista. Mientras no la elija se muestra
 * "Sin asignar" (antes se inventaba "Media" para todas).
 */
export type PrioridadEtiqueta = 'Urgente' | 'Alta' | 'Media' | 'Baja' | 'Sin asignar';

/** Opciones del menú, de mayor a menor. */
export const PRIORIDADES: { codigo: string; etiqueta: PrioridadEtiqueta }[] = [
  { codigo: 'URGENTE', etiqueta: 'Urgente' },
  { codigo: 'ALTA', etiqueta: 'Alta' },
  { codigo: 'MEDIA', etiqueta: 'Media' },
  { codigo: 'BAJA', etiqueta: 'Baja' }
];

export function formatearPrioridad(prioridad: string | null | undefined): PrioridadEtiqueta {
  switch (prioridad?.toUpperCase()) {
    case 'URGENTE':
      return 'Urgente';
    case 'ALTA':
      return 'Alta';
    case 'MEDIA':
      return 'Media';
    case 'BAJA':
      return 'Baja';
    default:
      return 'Sin asignar';
  }
}

/** 'Alta' -> 'ALTA'; 'Sin asignar' -> 'SIN_ASIGNAR' (así lo entiende el filtro del backend). */
export function codigoPrioridad(etiqueta: string | null | undefined): string {
  const p = PRIORIDADES.find(x => x.etiqueta === etiqueta);
  return p ? p.codigo : 'SIN_ASIGNAR';
}

/** Clase del chip de prioridad (styles.css: .prio-*). */
export function clasePrioridad(etiqueta: string | null | undefined): string {
  return 'prio-' + codigoPrioridad(etiqueta).toLowerCase().replace('_', '-');
}

/** '2026-09-20' o '2026-09-20T10:00:00' -> '20/09/2026'. */
export function formatearFecha(fecha: string | null | undefined): string {
  if (!fecha) return '';
  const [year, month, day] = fecha.substring(0, 10).split('-');
  return day && month && year ? `${day}/${month}/${year}` : fecha;
}

/** '2026-09-20T10:05:33' -> '20/09/2026 10:05'. */
export function formatearFechaHora(fecha: string | null | undefined): string {
  if (!fecha) return '';
  const hora = fecha.length >= 16 ? ' ' + fecha.substring(11, 16) : '';
  return formatearFecha(fecha) + hora;
}
