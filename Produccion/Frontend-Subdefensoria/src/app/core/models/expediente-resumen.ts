export interface ExpedienteResumen {
  expedienteId: number;
  folio: string;
  quejosoNombre: string;
  asunto: string;
  unidadAcademica: string;
  fechaAdmision: string;
  estatus: 'RECIBIDO' | 'EN_INVESTIGACION' | 'EN_GESTION_DIRECTOR' | 'LISTO_A_DICTAMINAR' | 'CONCLUIDO';
  oficioIdVigente: number | null;
  numeroOficioVigente: string | null;
  destinatarioNombreVigente: string | null;
  faseOficioVigente: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR' | null;
  estatusOficioVigente: 'EN_ESPERA' | 'VENCIDO' | null;
  diasTranscurridos: number | null;
  diasLimite: number | null;
}
