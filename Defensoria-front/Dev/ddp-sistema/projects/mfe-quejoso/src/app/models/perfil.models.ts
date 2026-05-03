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
}
