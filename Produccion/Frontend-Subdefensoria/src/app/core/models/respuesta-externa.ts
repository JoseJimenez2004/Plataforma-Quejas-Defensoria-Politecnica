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
  /** Archivo de la respuesta (oficio escaneado o PDF), si se adjuntó. */
  tieneArchivo?: boolean;
  archivoNombre?: string | null;
}

export interface RegistrarRespuestaExternaPayload {
  oficioId: number;
  canalRecepcion: string;
  numeroOficioRespuestaUA?: string;
  resumen: string;
}
