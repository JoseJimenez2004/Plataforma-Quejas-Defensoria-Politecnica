export interface AcuerdoConclusion {
  id: number;
  expedienteId: number;
  folio: string;
  textoAcuerdo: string;
  concluido: boolean;
  fechaCreacion: string;
  fechaEnvioSecretarial: string | null;
  estatusExpediente: string;
}

export interface CrearAcuerdoConclusionPayload {
  expedienteId: number;
  textoAcuerdo: string;
  concluir: boolean;
}
