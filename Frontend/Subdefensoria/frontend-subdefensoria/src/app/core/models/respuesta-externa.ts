export interface RespuestaExterna {
  id: number;
  expedienteId: number;
  oficioId: number;
  folio: string;
  canalRecepcion: string;
  numeroOficioRespuestaUA: string | null;
  archivoPdfPath: string | null;
  resumen: string;
  fechaRecepcion: string;
  estatusExpediente: string;
}

export interface RegistrarRespuestaExternaPayload {
  oficioId: number;
  canalRecepcion: string;
  numeroOficioRespuestaUA?: string;
  resumen: string;
}
