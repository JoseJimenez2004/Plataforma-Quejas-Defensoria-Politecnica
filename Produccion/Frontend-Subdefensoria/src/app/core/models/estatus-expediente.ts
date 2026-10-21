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

export const ETIQUETAS_ESTATUS: Record<EstatusExpediente, string> = {
  RECIBIDO: 'Recibido',
  EN_INVESTIGACION: 'En investigación',
  EN_ESPERA_OFICIO: 'En espera de oficio',
  ELABORO_ACUERDO: 'Elaboró acuerdo',
  PENDIENTE_CONCILIACION: 'Pendiente de acuerdo de conciliación',
  PENDIENTE_CONCLUSION: 'Pendiente de conclusión',
  CONCLUIDO: 'Concluido'
};

export function etiquetaEstatus(estatus: string | null | undefined): string {
  return estatus ? ETIQUETAS_ESTATUS[estatus as EstatusExpediente] ?? estatus : '';
}
