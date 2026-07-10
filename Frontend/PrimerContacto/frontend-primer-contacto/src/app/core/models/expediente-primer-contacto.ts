export interface ExpedientePrimerContacto {
  quejaId: number;
  folio: string;
  descripcionHechos: string;
  fechaRecepcion: string;
  estatus: string;
  prioridad: string;
  quejoso: {
    id: number;
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
    quejaId: number;
    folio: string;
    analistaId: number;
    analistaNombre: string;
    contenido: string;
    fechaCreacion: string;
    fechaActualizacion: string;
  }[];
}