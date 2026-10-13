export interface AcuerdoConclusion {
  id: number;
  expedienteId: number;
  folio: string;
  textoAcuerdo: string;
  concluido: boolean;
  fechaCreacion: string;
  fechaEnvioSecretarial: string | null;
  estatusExpediente: string;
  /** Respuesta del quejoso al acuerdo de conciliación: ACEPTADO | RECHAZADO. */
  respuestaQuejoso?: 'ACEPTADO' | 'RECHAZADO' | null;
  comentarioQuejoso?: string | null;
  fechaRespuestaQuejoso?: string | null;
  /** Acuerdo de conclusión (después de que el quejoso aceptó). */
  textoConclusion?: string | null;
  fechaConclusion?: string | null;
}

export interface CrearAcuerdoConclusionPayload {
  expedienteId: number;
  textoAcuerdo: string;
  concluir: boolean;
}
