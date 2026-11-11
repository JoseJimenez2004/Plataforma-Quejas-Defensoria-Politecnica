export interface Remision {
  id?: number;
  expedienteId: number;
  folio: string;

  analistaId: number;
  analistaNombre: string;

  autoridadRemision: string;
  justificacionLegal: string;

  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;

  fechaRemision?: string;

  /** GENERADA (flujo anterior: oficio sin enviar) | ENVIADA (el correo salió). */
  estatus: 'GENERADA' | 'ENVIADA';
  numeroOficio?: string;
  fechaEnvio?: string;

  /** OAG | OIC | COSECOVI (null en las remisiones anteriores al catálogo). */
  instanciaClave?: string;
  /** Correo al que salió el oficio. */
  correoDestino?: string;
}

/** Instancia del catálogo (la Defensora administra el correo). */
export interface InstanciaRemision {
  clave: string;
  sigla: string;
  nombre: string;
  correo?: string;
}

export interface CrearRemisionPayload {
  folio: string;

  instanciaClave: string;
  justificacionLegal: string;

  sugerenciaQuejoso?: string;
  adjuntarExpediente: boolean;
}
