import { NotaAnalisis } from './nota-analisis.model';

export interface EvidenciaDetalle {
  /** Id de la evidencia original (queja_evidencias): con él se abre el archivo. */
  id: number;
  nombre: string;
  tipo: string;
}

export interface ExpedienteDetalle {
  expedienteId?: number;
  folio: string;
  folioOrigen?: string;
  folioSubdefensoria?: string;

  asunto: string;
  fechaIngreso: string;

  /** Etiqueta legible ('En análisis') y código del backend ('EN_ANALISIS'). */
  estatus: string;
  estatusCodigo: string;

  prioridad: 'Alta' | 'Media' | 'Baja';
  narrativa: string;

  /** Quién abrió el expediente y cuándo (TURNADA -> EN_ANALISIS). */
  fechaInicioAnalisis?: string;
  analistaAnalisisNombre?: string;

  quejoso: {
    nombre: string;
    boleta: string;
    correo: string;
    telefono: string;
    unidadAcademica: string;
  };

  evidencias: EvidenciaDetalle[];

  /** Notas completas: autor y fecha incluidos (antes se aplanaban a texto). */
  notas: NotaAnalisis[];
}
