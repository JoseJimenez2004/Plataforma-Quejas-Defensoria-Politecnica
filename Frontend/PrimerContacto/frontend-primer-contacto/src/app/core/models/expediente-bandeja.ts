export interface ExpedienteBandeja {
  quejaId: number;
  folio: string;
  nombreQuejoso: string;
  unidadAcademica: string;
  tema: string;
  prioridad: 'Alta' | 'Media' | 'Baja';
  estatus: string;
  fechaRecepcion: string;
}