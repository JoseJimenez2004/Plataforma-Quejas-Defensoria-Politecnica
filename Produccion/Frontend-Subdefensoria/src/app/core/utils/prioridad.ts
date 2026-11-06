/**
 * SD-6 (2026-11): la prioridad la asigna Primer Contacto y llega con el expediente; el
 * subdefensor también puede cambiarla (el cambio no regresa a Primer Contacto).
 * Expedientes que llegaron antes quedan "Sin asignar". Mismos colores que en PC.
 */
export const PRIORIDADES: { codigo: string; etiqueta: string }[] = [
  { codigo: 'URGENTE', etiqueta: 'Urgente' },
  { codigo: 'ALTA', etiqueta: 'Alta' },
  { codigo: 'MEDIA', etiqueta: 'Media' },
  { codigo: 'BAJA', etiqueta: 'Baja' }
];

/** Urgente -> Alta -> Media -> Baja -> Sin asignar. */
export const ORDEN_PRIORIDAD: Record<string, number> = { URGENTE: 0, ALTA: 1, MEDIA: 2, BAJA: 3 };

export function codigoPrioridad(p: string | null | undefined): string {
  const c = (p ?? '').trim().toUpperCase();
  return c in ORDEN_PRIORIDAD ? c : 'SIN_ASIGNAR';
}

export function rangoPrioridad(p: string | null | undefined): number {
  return ORDEN_PRIORIDAD[codigoPrioridad(p)] ?? 4;
}

export function etiquetaPrioridad(p: string | null | undefined): string {
  return PRIORIDADES.find(x => x.codigo === codigoPrioridad(p))?.etiqueta ?? 'Sin asignar';
}

export function clasePrioridad(p: string | null | undefined): string {
  return 'prio-' + codigoPrioridad(p).toLowerCase().replace('_', '-');
}
