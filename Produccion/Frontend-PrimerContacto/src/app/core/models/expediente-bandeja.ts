export interface ExpedienteBandeja {
  expedienteId: number;
  folio: string;
  folioOrigen?: string;

  nombreQuejoso: string;
  unidadAcademica: string;
  tema: string;

  prioridad: 'Alta' | 'Media' | 'Baja';

  /** Etiqueta legible ('En análisis') y código del backend ('EN_ANALISIS'). */
  estatus: string;
  estatusCodigo: string;

  fechaRecepcion: string;

  /** Indicador aparte (no es un estado): tiene cita programada o confirmada. */
  tieneCitaActiva: boolean;
}

/** Cuerpo de POST /bandeja/filtrar (CU-PC-02). */
export interface FiltroBandeja {
  texto?: string;
  prioridades?: string[];
  estatusLista?: string[];
  unidadesAcademicas?: string[];
  temas?: string[];
  orden?: 'recientes' | 'antiguos';
}
