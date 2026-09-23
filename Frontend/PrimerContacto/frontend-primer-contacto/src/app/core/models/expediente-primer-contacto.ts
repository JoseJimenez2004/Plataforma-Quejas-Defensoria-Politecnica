export interface ExpedientePrimerContacto {
  expedienteId: number;
  folio: string;
  folioOrigen?: string;
  folioSubdefensoria?: string;

  tema: string;
  descripcionHechos: string;
  fechaRecepcion: string;
  estatus: string;
  prioridad: string;

  quejoso: {
    id?: number;
    nombreCompleto: string;
    correo: string;
    telefono: string;
    unidadAcademica: string;
    tipoUsuario: string;
  };

  evidencias?: {
    id: number;
    nombreArchivo: string;
    tipoArchivo: string;
    urlArchivo: string;
    fechaCarga: string;
  }[];

  notas?: {
    id: number;
    expedienteId: number;
    folio: string;
    analistaId: number;
    analistaNombre: string;
    contenido: string;
    fechaCreacion: string;
    fechaActualizacion?: string;
  }[];
}