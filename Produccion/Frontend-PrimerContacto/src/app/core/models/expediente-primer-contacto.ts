/** Datos de la queja original (lo que capturó el quejoso). Solo lectura. */
export interface DatosQueja {
  fechaRegistro?: string;
  asunto?: string;
  fechaHechos?: string;
  unidadAcademicaClave?: string;
  tipoIdentificacionQuejoso?: string;
  numeroIdentificacionQuejoso?: string;
  fechaNacimientoQuejoso?: string;
  denunciado?: string;
}

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

  fechaInicioAnalisis?: string;
  analistaAnalisisNombre?: string;

  quejoso: {
    id?: number;
    nombreCompleto: string;
    correo: string;
    telefono: string;
    unidadAcademica: string;
    tipoUsuario: string;
  };

  datosQueja?: DatosQueja;

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