export interface Oficio {
  id: number;
  expedienteId: number;
  folio: string;
  numeroOficio: string;
  fase: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR';
  destinatarioNombre: string;
  destinatarioCorreo: string;
  unidadAcademica: string;
  contenidoRedactado: string;
  tipoPlazo: 'PRIMERA' | 'SUBSECUENTE';
  fechaEnvio: string;
  fechaLimite: string;
  estatus: 'EN_ESPERA' | 'VENCIDO' | 'RESPONDIDO';
}

export interface CrearOficioPayload {
  expedienteId: number;
  destinatarioNombre: string;
  destinatarioCorreo: string;
  unidadAcademica: string;
  contenidoRedactado: string;
}
