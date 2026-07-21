export interface AlertaVencimiento {
  oficioId: number;
  numeroOficio: string;
  folio: string;
  unidadAcademica: string;
  fase: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR';
  fechaLimite: string;
  diasRetraso: number;
}
