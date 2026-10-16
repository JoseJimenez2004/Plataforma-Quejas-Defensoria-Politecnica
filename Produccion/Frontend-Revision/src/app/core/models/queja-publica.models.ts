/** Tipos del formulario de queja (copiados de Frontend/core/models/queja.models.ts) que usa el
 * registro desde ventanilla. El registro va al MISMO endpoint que el formulario público. */
export interface QuejaTutor {
  nombre: string;
  apellido1: string;
  apellido2?: string;
  parentesco: string;
  correo?: string;
  telefono?: string;
}

/** Lo que regresa queja-service al registrar (misma forma que en el portal). */
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
  apellido1Quejoso?: string;
  apellido2Quejoso?: string;
  fechaNacimientoQuejoso?: string;
  tipoIdentificacionQuejoso?: 'alumno' | 'empleado';
  numeroIdentificacionQuejoso?: string;
  unidadAcademicaClave?: string;
  fechaHechos?: string;
  nombreDenunciado?: string;
  apellido1Denunciado?: string;
  /** Segundo apellido del denunciado — opcional, el quejoso puede no conocerlo. */
  apellido2Denunciado?: string;
  /** "AUTENTICADO" | "PUBLICO" */
  origenRegistro?: string;
  /** Motivo por el que la Defensoría no admitió la queja — solo cuando estatus = RECHAZADA.
   * Lo escribe el recepcionista desde revision-service; el backend ya lo devolvía, pero el
   * modelo del frontend no lo declaraba, así que el detalle nunca lo mostraba. */
  motivoRechazo?: string;
  /** Área a la que se turnó la queja — solo cuando estatus = TURNADA. */
  areaTurnada?: string;
  fechaTurnado?: string;
  /** Constancia de aceptación del aviso de privacidad (solo en quejas del formulario público). */
  avisoPrivacidadAceptado?: boolean;
  avisoPrivacidadFecha?: string;
  avisoPrivacidadVersion?: string;
  tutor?: QuejaTutor;
  /** "RECIBIDA" | "EN_REVISION" | "FINALIZADA" — puede venir null en quejas viejas
   * (creadas antes de que este campo existiera); se trata como "RECIBIDA" en ese caso. */
  estatus?: string;
}

/** Metadatos de un archivo de evidencia (sin su contenido binario). */
export interface RegistroQuejaPublicaRequest {
  nombre: string;
  apellido1: string;
  apellido2?: string;
  correo: string;
  fechaNacimiento: string;
  tipoIdentificacion: 'alumno' | 'empleado';
  numeroIdentificacion: string;
  unidadAcademicaClave: string;
  fechaHechos: string;
  nombreDenunciado?: string;
  apellido1Denunciado?: string;
  apellido2Denunciado?: string;
  descripcion: string;
  archivos?: File[];
  avisoPrivacidadAceptado: boolean;
  avisoPrivacidadVersion: string;
  tutor?: QuejaTutor;
}

/** Datos propios de la recepción física (se guardan en la queja ya creada). */
export interface DatosVentanilla {
  tipoDocumento: string;
  numeroOficio?: string;
  fechaRecepcionFisica: string;
  ubicacionFisica?: string;
}
