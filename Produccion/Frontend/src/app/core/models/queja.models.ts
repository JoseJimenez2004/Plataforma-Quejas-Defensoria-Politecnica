export interface QuejaTutor {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  parentesco: string;
  correo?: string;
  telefono?: string;
}

export interface Queja {
  id: number;
  numeroFolio: string;
  correoInstitucional: string;
  motivo: string;
  descripcion: string;
  /** @deprecated ya no se usa — las evidencias nuevas se guardan en BD (ver QuejaEvidencia
   * en el backend), no en disco. Se deja el campo por compatibilidad con quejas viejas. */
  rutaEvidencia?: string;
  fechaCreacion: string;

  // Datos estructurados del quejoso y de la queja (antes iban como texto libre dentro de
  // "descripcion"; ahora son columnas propias en el backend).
  nombreQuejoso?: string;
  apellidoPaternoQuejoso?: string;
  apellidoMaternoQuejoso?: string;
  fechaNacimientoQuejoso?: string;
  tipoIdentificacionQuejoso?: 'alumno' | 'empleado';
  numeroIdentificacionQuejoso?: string;
  unidadAcademicaClave?: string;
  fechaHechos?: string;
  nombreDenunciado?: string;
  apellidoDenunciado?: string;
  /** "AUTENTICADO" | "PUBLICO" */
  origenRegistro?: string;
  tutor?: QuejaTutor;
  /** "RECIBIDA" | "EN_REVISION" | "FINALIZADA" — puede venir null en quejas viejas
   * (creadas antes de que este campo existiera); se trata como "RECIBIDA" en ese caso. */
  estatus?: string;
}

/** Metadatos de un archivo de evidencia (sin su contenido binario). */
export interface EvidenciaResumen {
  id: number;
  nombreArchivo: string;
  tipoMime?: string;
  tamanioBytes?: number;
  fechaSubida?: string;
}

export interface ValidarFolioRequest {
  folio: string;
  correo: string;
}

/** Payload del formulario público de registro (sin sesión iniciada). */
export interface RegistroQuejaPublicaRequest {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  correo: string;
  fechaNacimiento: string;
  tipoIdentificacion: 'alumno' | 'empleado';
  numeroIdentificacion: string;
  unidadAcademicaClave: string;
  fechaHechos: string;
  nombreDenunciado?: string;
  apellidoDenunciado?: string;
  descripcion: string;
  archivos?: File[];
  tutor?: QuejaTutor;
}

/** Estatus visual de una queja. */
export type EstatusQueja = 'Recibida' | 'En Revisión' | 'Finalizada';

/** El backend guarda el estatus como código plano ("RECIBIDA" | "EN_REVISION" |
 * "FINALIZADA" | null en quejas viejas sin este campo) — esto lo traduce a la etiqueta
 * legible que ya usa toda la UI. */
export function etiquetaEstatus(estatus: string | null | undefined): EstatusQueja {
  switch (estatus) {
    case 'EN_REVISION':
      return 'En Revisión';
    case 'FINALIZADA':
      return 'Finalizada';
    case 'RECIBIDA':
    default:
      return 'Recibida';
  }
}
