/**
 * Estatus del expediente de Subdefensoría, alineados a la tabla ECA del diagrama de estados
 * de la queja (filas 12 a 20). Ver EstatusExpediente.java en el backend.
 *
 *   RECIBIDO -> EN_INVESTIGACION -> EN_ESPERA_OFICIO -> ELABORO_ACUERDO
 *     -> PENDIENTE_CONCILIACION -> PENDIENTE_CONCLUSION -> CONCLUIDO
 *
 * Dos rondas de oficio: la respuesta a la solicitud de información regresa a
 * EN_INVESTIGACION para el oficio de gestión con el director. Si el quejoso rechaza el
 * acuerdo de conciliación, también regresa a EN_INVESTIGACION.
 */
export type EstatusExpediente =
  | 'RECIBIDO'
  | 'EN_INVESTIGACION'
  | 'EN_ESPERA_OFICIO'
  | 'ELABORO_ACUERDO'
  | 'PENDIENTE_CONCILIACION'
  | 'PENDIENTE_CONCLUSION'
  | 'CONCLUIDO';

/** Orden del flujo, para la barra de pasos. */
export const PASOS_EXPEDIENTE: EstatusExpediente[] = [
  'RECIBIDO',
  'EN_INVESTIGACION',
  'EN_ESPERA_OFICIO',
  'ELABORO_ACUERDO',
  'PENDIENTE_CONCILIACION',
  'PENDIENTE_CONCLUSION',
  'CONCLUIDO'
];

/**
 * SD-4 (2026-11): los nombres dicen qué falta y quién lo tiene. Solo cambia el texto que se
 * ve; los códigos (ELABORO_ACUERDO, etc.) siguen igual en el front y en la base.
 */
export const ETIQUETAS_ESTATUS: Record<EstatusExpediente, string> = {
  RECIBIDO: 'Por abrir',
  EN_INVESTIGACION: 'Por solicitar oficio',
  EN_ESPERA_OFICIO: 'Esperando respuesta de la unidad',
  ELABORO_ACUERDO: 'Por redactar acuerdo de conciliación',
  PENDIENTE_CONCILIACION: 'Esperando respuesta del quejoso',
  PENDIENTE_CONCLUSION: 'Por redactar acuerdo de conclusión',
  CONCLUIDO: 'Concluido'
};

export function etiquetaEstatus(estatus: string | null | undefined): string {
  return estatus ? ETIQUETAS_ESTATUS[estatus as EstatusExpediente] ?? estatus : '';
}
