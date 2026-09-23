export interface ExpedienteBandeja {
  expedienteId: number;
  folio: string;
  folioOrigen?: string;

  nombreQuejoso: string;
  unidadAcademica: string;
  tema: string;

  prioridad: 'Alta' | 'Media' | 'Baja';
  estatus: string;
  fechaRecepcion: string;
}