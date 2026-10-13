import { EstatusExpediente } from './estatus-expediente';

export interface ExpedienteResumen {
  expedienteId: number;
  folio: string;
  quejosoNombre: string;
  asunto: string;
  unidadAcademica: string;
  fechaAdmision: string;
  estatus: EstatusExpediente;
  oficioIdVigente: number | null;
  numeroOficioVigente: string | null;
  destinatarioNombreVigente: string | null;
  faseOficioVigente: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR' | null;
  estatusOficioVigente: 'EN_ESPERA' | 'VENCIDO' | null;
  diasTranscurridos: number | null;
  diasLimite: number | null;
  /** Solo si toca emitir oficio. */
  siguienteFase?: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR' | null;
}
