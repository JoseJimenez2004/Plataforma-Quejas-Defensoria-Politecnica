/** Correo con el que Primer Contacto le pidió al denunciado su versión de los hechos. */
export interface SolicitudDenunciado {
  id: number;
  folio: string;
  folioQueja: string;
  correoDenunciado: string;
  /** false si el correo no se pudo enviar: se puede reenviar. */
  correoEnviado: boolean;
  enviadoPorNombre?: string;
  fechaEnvio: string;
}

/** Archivo de la respuesta (credencial o evidencia), sin el contenido. */
export interface ArchivoRespuestaDenunciado {
  id: number;
  tipo: string;
  nombreArchivo: string;
  tipoMime: string;
  tamanioBytes: number;
  fechaSubida: string;
}

/** Respuesta del denunciado tal como la guarda denunciado-service. */
export interface RespuestaDenunciado {
  id: number;
  folioRespuesta: string;
  folioQueja: string;
  nombre: string;
  apellido1: string;
  apellido2?: string;
  unidadProcedenciaClave: string;
  tipoIdentificacion: string;
  numeroIdentificacion: string;
  descripcionHechos: string;
  estatus: string;
  fechaRegistro: string;
  avisoPrivacidadVersion?: string;
  archivos: ArchivoRespuestaDenunciado[];
}
