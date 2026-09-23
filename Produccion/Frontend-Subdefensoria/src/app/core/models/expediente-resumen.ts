export interface ExpedienteResumen {
  expedienteId: number;
  folio: string;
  quejosoNombre: string;
  asunto: string;
  unidadAcademica: string;
  fechaAdmision: string;
  estatus: 'RECIBIDO' | 'EN_INVESTIGACION' | 'EN_ESPERA_OFICIO' | 'ELABORO_ACUERDO' | 'PENDIENTE_CONCLUSION' | 'CONCLUIDO';
  oficioIdVigente: number | null;
  numeroOficioVigente: string | null;
  destinatarioNombreVigente: string | null;
  faseOficioVigente: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR' | null;
  estatusOficioVigente: 'EN_ESPERA' | 'VENCIDO' | null;
  diasTranscurridos: number | null;
  diasLimite: number | null;
}
