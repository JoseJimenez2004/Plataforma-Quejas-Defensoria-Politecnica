export interface ExpedienteDetalle {
  quejaId?: number;
  folio: string;
  asunto: string;
  fechaIngreso: string;
  estatus: string;
  prioridad: 'Alta' | 'Media' | 'Baja';
  narrativa: string;
  quejoso: {
    nombre: string;
    boleta: string;
    correo: string;
    telefono: string;
    unidadAcademica: string;
  };
  evidencias: {
    nombre: string;
    tipo: string;
  }[];
  notas: string[];
}