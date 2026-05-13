export interface TramitesResumenDTO {
  totales: number;
  enProceso: number;
  finalizadas: number;
  accionesPendientes?: number;
}

export interface ConciliacionDTO {
  folioQueja: string;
  tituloAcuerdo: string;
  contenidoHtml: string;
  fechaPropuesta: string;
  urlPdf: string;
}

export interface AcuerdoPendienteDTO {
  folio: string;
  fechaRegistro: string;
  asunto: string;
  unidadAcademica: string;
}
