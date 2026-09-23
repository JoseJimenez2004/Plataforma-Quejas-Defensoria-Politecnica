export interface Recordatorio {
  id: number;
  oficioId: number;
  numeroOficio: string;
  mensaje: string;
  medidasOfrecidas: string | null;
  diasRetraso: number;
  fechaEnvio: string;
  nuevaFechaLimite: string;
  estatusExpediente: string;
}

export interface GenerarRecordatorioPayload {
  oficioId: number;
  mensaje: string;
  medidasOfrecidas?: string;
}
