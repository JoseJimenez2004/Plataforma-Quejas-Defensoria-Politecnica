export enum EstatusQueja {
  RECIBIDA = 'RECIBIDA',
  EN_ANALISIS = 'EN_ANALISIS',
  COMPETENTE = 'COMPETENTE',
  IMPROCEDENTE = 'IMPROCEDENTE',
  ASIGNADO_A_ABOGADO = 'ASIGNADO_A_ABOGADO',
  EN_INVESTIGACION = 'EN_INVESTIGACION',
  PROCESO_CONCLUSION = 'PROCESO_CONCLUSION',
  FINALIZADA = 'FINALIZADA',
  REMISION = 'REMISION',
  CANCELADA = 'CANCELADA',
}

export const ESTATUS_LABEL: Record<EstatusQueja, string> = {
  [EstatusQueja.RECIBIDA]: 'Recibida',
  [EstatusQueja.EN_ANALISIS]: 'En Análisis',
  [EstatusQueja.COMPETENTE]: 'Competente',
  [EstatusQueja.IMPROCEDENTE]: 'Improcedente',
  [EstatusQueja.ASIGNADO_A_ABOGADO]: 'Asignado a Abogado',
  [EstatusQueja.EN_INVESTIGACION]: 'En Investigación',
  [EstatusQueja.PROCESO_CONCLUSION]: 'Proceso de Conclusión',
  [EstatusQueja.FINALIZADA]: 'Finalizada',
  [EstatusQueja.REMISION]: 'Remisión',
  [EstatusQueja.CANCELADA]: 'Cancelada',
};

export interface TutorDTO {
  nombre: string;
  primerApellido: string;
  segundoApellido: string;
  parentesco: string;
  correo: string;
  telefono: string;
}

export interface QuejaRegistroDTO {
  nombreQuejoso: string;
  primerApellido: string;
  segundoApellido: string;
  correo: string;
  fechaNacimiento: string;
  identificacionInstitucional: string;
  tipoIdentificacion: string;
  unidadAcademica: string;
  fechaHechos: string;
  asunto: string;
  descripcionHechos: string;
  nombreDenunciado: string;
  primerApellidoDenunciado: string;
  segundoApellidoDenunciado?: string;
  tutor?: TutorDTO;
}

export interface QuejaEditarDTO {
  asunto: string;
  descripcionHechos: string;
  evidenciasBorrarIds: number[];
}

export interface EvidenciaDTO {
  id: number;
  nombreArchivo: string;
  urlDescarga: string;
}

export interface QuejaSeguimientoDTO {
  folio: string;
  fechaRegistro: string;
  asunto: string;
  descripcionHechos: string;
  estatusActual: EstatusQueja;
  evidencias: EvidenciaDTO[];
}

export interface QuejaFiltroDTO {
  folio?: string;
  estatus?: EstatusQueja | '';
  fechaInicio?: string;
  fechaFin?: string;
}
