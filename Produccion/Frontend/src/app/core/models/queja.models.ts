export interface QuejaTutor {
  nombre: string;
  apellido1: string;
  apellido2?: string;
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
export interface EvidenciaResumen {
  id: number;
  nombreArchivo: string;
  tipoMime?: string;
  tamanioBytes?: number;
  fechaSubida?: string;
  /** "IDENTIFICACION" (credencial oficial) | "EVIDENCIA" | undefined en registros viejos. */
  tipo?: string;
}

export interface ValidarFolioRequest {
  folio: string;
  correo: string;
}

/** Payload del formulario público de registro (sin sesión iniciada). */
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
  /** El formulario obliga a leer el aviso hasta el final; el backend lo exige en true. */
  avisoPrivacidadAceptado: boolean;
  avisoPrivacidadVersion: string;
  tutor?: QuejaTutor;
}

/** Body de PUT /api/quejoso/quejas/mias/{folio} — solo se puede editar mientras la queja
 * sigue en estatus "RECIBIDA" (el backend lo valida también). */
export interface EditarQuejaRequest {
  descripcion: string;
  unidadAcademicaClave?: string;
  fechaHechos?: string;
  nombreDenunciado?: string;
  apellido1Denunciado?: string;
  apellido2Denunciado?: string;
}

/**
 * Códigos de estatus que realmente emite el backend: los del diagrama de estados de la queja.
 *
 * OJO: antes esta lista decía RECIBIDA / EN_REVISION / FINALIZADA, que no existen en el
 * backend salvo la primera. Como el traductor caía en `default: 'Recibida'`, una queja
 * RECHAZADA o TURNADA se le mostraba al quejoso como "Recibida" — es decir, el panel le
 * mentía sobre el estado de su trámite.
 *
 * Recepción (revision-service / queja-service): RECIBIDA … TURNADA, RECHAZADA, CORREGIDA,
 * CANCELADA. Desde 2026-10-13 Primer Contacto (EN_ANALISIS … REMITIDA) y Subdefensoría
 * (RECIBIDA_SUBDEFENSORIA … CONCLUIDA) también escriben su avance en quejas.estatus.
 */
export type CodigoEstatus =
  | 'RECIBIDA'
  | 'EN_VALIDACION'
  | 'TURNADA'
  | 'RECHAZADA'
  | 'CORREGIDA'
  | 'CANCELADA'
  | 'EN_ANALISIS'
  | 'PROCEDENTE'
  | 'IMPROCEDENTE'
  | 'REMITIDA'
  | 'RECIBIDA_SUBDEFENSORIA'
  | 'EN_INVESTIGACION'
  | 'EN_ESPERA_OFICIO'
  | 'ELABORO_ACUERDO'
  | 'PENDIENTE_CONCILIACION'
  | 'PENDIENTE_CONCLUSION'
  | 'CONCLUIDA';

/** Etiqueta legible de cada estatus (lo que ve el quejoso). */
const ETIQUETAS: Record<CodigoEstatus, string> = {
  RECIBIDA: 'Recibida',
  EN_VALIDACION: 'En validación',
  TURNADA: 'Turnada',
  RECHAZADA: 'Rechazada',
  CORREGIDA: 'Corregida',
  CANCELADA: 'Cancelada',
  EN_ANALISIS: 'En análisis',
  PROCEDENTE: 'Procedente',
  IMPROCEDENTE: 'Improcedente',
  REMITIDA: 'Remitida',
  RECIBIDA_SUBDEFENSORIA: 'En Subdefensoría',
  EN_INVESTIGACION: 'En investigación',
  EN_ESPERA_OFICIO: 'Esperando información',
  ELABORO_ACUERDO: 'Elaborando acuerdo',
  PENDIENTE_CONCILIACION: 'Acuerdo por responder',
  PENDIENTE_CONCLUSION: 'Por concluir',
  CONCLUIDA: 'Concluida',
};

export function etiquetaEstatus(estatus: string | null | undefined): string {
  // Las quejas creadas antes de que existiera la columna traen null: se tratan como
  // recibidas, que es el estado en el que nacen.
  return ETIQUETAS[(estatus ?? 'RECIBIDA') as CodigoEstatus] ?? 'Recibida';
}

/** Clase CSS de la insignia de estatus — separa el color del texto. Los estados de Primer
 * Contacto y Subdefensoría reutilizan los colores existentes (no se agregan estilos). */
export function claseEstatus(estatus: string | null | undefined): string {
  switch (estatus) {
    case 'EN_VALIDACION':
    case 'CORREGIDA':
      return 'badge--validacion';
    case 'TURNADA':
      return 'badge--turnada';
    case 'RECHAZADA':
    case 'IMPROCEDENTE':
      return 'badge--rechazada';
    case 'CANCELADA':
      return 'badge--cancelada';
    case 'REMITIDA':
      return 'badge--cancelada';
    case 'PENDIENTE_CONCILIACION':
      return 'badge--validacion';
    case 'CONCLUIDA':
      return 'badge--turnada';
    case 'EN_ANALISIS':
    case 'PROCEDENTE':
    case 'RECIBIDA_SUBDEFENSORIA':
    case 'EN_INVESTIGACION':
    case 'EN_ESPERA_OFICIO':
    case 'ELABORO_ACUERDO':
    case 'PENDIENTE_CONCLUSION':
      return 'badge--turnada';
    default:
      return 'badge--recibida';
  }
}

/** true mientras el quejoso pueda editar, agregar evidencias o retirar su queja. */
export function esEditable(estatus: string | null | undefined): boolean {
  return !estatus || estatus === 'RECIBIDA';
}

/**
 * Estatus en los que el trámite ya terminó, para agrupar en el historial.
 *
 * RECHAZADA ya NO cuenta como cerrada: desde el diagrama de estados del 2026-09-18, el
 * quejoso puede corregir las observaciones y reenviar la queja, así que es un estado del
 * que todavía se sale. Los finales son: retirada, remitida a otra instancia o concluida.
 */
export function estaCerrada(estatus: string | null | undefined): boolean {
  return estatus === 'CANCELADA' || estatus === 'REMITIDA' || estatus === 'CONCLUIDA';
}

/** true cuando la queja fue rechazada y el quejoso todavía puede corregirla y reenviarla. */
export function puedeCorregirse(estatus: string | null | undefined): boolean {
  return estatus === 'RECHAZADA';
}

// ============================================================================================
// Línea del tiempo del trámite
// ============================================================================================

export interface PasoTramite {
  clave: string;
  titulo: string;
  descripcion: string;
  estado: 'completado' | 'actual' | 'pendiente' | 'cancelado';
}

/** Pasos del camino principal del diagrama, de la recepción a la conclusión. */
const PASOS: Omit<PasoTramite, 'estado'>[] = [
  { clave: 'RECIBIDA', titulo: 'Queja recibida', descripcion: 'Tu queja se registró y tiene folio.' },
  { clave: 'EN_VALIDACION', titulo: 'En validación', descripcion: 'La Defensoría revisa que la documentación esté completa.' },
  { clave: 'TURNADA', titulo: 'Turnada a Primer Contacto', descripcion: 'Tu queja pasó al área que analiza si le corresponde a la Defensoría.' },
  { clave: 'EN_ANALISIS', titulo: 'En análisis', descripcion: 'Primer Contacto analiza tu caso; puede citarte para conocer los hechos.' },
  { clave: 'EN_INVESTIGACION', titulo: 'En investigación', descripcion: 'Subdefensoría investiga los hechos y solicita información a la unidad académica.' },
  { clave: 'ACUERDO', titulo: 'Acuerdo de conciliación', descripcion: 'Se te propone un acuerdo para resolver tu caso.' },
  { clave: 'CONCLUIDA', titulo: 'Caso concluido', descripcion: 'La Defensoría emitió el acuerdo de conclusión.' },
];

/** En qué paso va cada estatus, y qué decir de él cuando es el paso actual. */
const POSICION: Record<string, { paso: number; detalle?: string }> = {
  RECIBIDA: { paso: 0 },
  EN_VALIDACION: { paso: 1 },
  CORREGIDA: { paso: 1, detalle: 'Corregiste tu queja; la Defensoría la vuelve a revisar.' },
  TURNADA: { paso: 2 },
  EN_ANALISIS: { paso: 3 },
  PROCEDENTE: { paso: 4, detalle: 'Tu queja es competencia de la Defensoría y pasa a Subdefensoría para investigarse.' },
  RECIBIDA_SUBDEFENSORIA: { paso: 4, detalle: 'Subdefensoría recibió tu caso y comenzará la investigación.' },
  EN_INVESTIGACION: { paso: 4 },
  EN_ESPERA_OFICIO: { paso: 4, detalle: 'Se solicitó información a la unidad académica y se espera su respuesta.' },
  ELABORO_ACUERDO: { paso: 4, detalle: 'Con la información recibida se elabora un acuerdo para tu caso.' },
  PENDIENTE_CONCILIACION: { paso: 5, detalle: 'Te enviamos un acuerdo de conciliación: revísalo y respóndelo.' },
  PENDIENTE_CONCLUSION: { paso: 5, detalle: 'Aceptaste el acuerdo; se prepara el acuerdo de conclusión.' },
  CONCLUIDA: { paso: 6 },
};

/**
 * Arma la línea del tiempo que ve el quejoso, con un paso por etapa del diagrama de estados.
 *
 * RECHAZADA, CANCELADA e IMPROCEDENTE/REMITIDA no son un paso más: son caminos alternativos,
 * y por eso cortan la línea en vez de avanzarla.
 */
export function pasosDelTramite(estatus: string | null | undefined): PasoTramite[] {
  const codigo = estatus ?? 'RECIBIDA';

  if (codigo === 'RECHAZADA' || codigo === 'CANCELADA') {
    const cancelado = codigo === 'CANCELADA';
    return [
      { ...PASOS[0], estado: 'completado' },
      {
        clave: codigo,
        titulo: cancelado ? 'Queja retirada' : 'Queja rechazada',
        descripcion: cancelado
          ? 'Retiraste esta queja. El registro se conserva en la Defensoría.'
          : 'La Defensoría no admitió la queja. Revisa el motivo más abajo.',
        estado: 'cancelado',
      },
    ];
  }

  if (codigo === 'IMPROCEDENTE' || codigo === 'REMITIDA') {
    const remitida = codigo === 'REMITIDA';
    return [
      ...PASOS.slice(0, 4).map((p) => ({ ...p, estado: 'completado' as const })),
      {
        clave: codigo,
        titulo: remitida ? 'Remitida a otra instancia' : 'No es competencia de la Defensoría',
        descripcion: remitida
          ? 'Tu caso se envió a la instancia que puede atenderlo; te avisamos por correo a cuál.'
          : 'Se prepara la remisión de tu caso a la instancia que puede atenderlo.',
        estado: remitida ? 'completado' : 'actual',
      },
    ];
  }

  const posicion = POSICION[codigo] ?? POSICION['RECIBIDA'];
  const concluida = codigo === 'CONCLUIDA';

  return PASOS.map((paso, i) => {
    const estado: PasoTramite['estado'] =
      i < posicion.paso || (concluida && i === posicion.paso)
        ? 'completado'
        : i === posicion.paso
          ? 'actual'
          : 'pendiente';
    return {
      ...paso,
      descripcion: estado === 'actual' && posicion.detalle ? posicion.detalle : paso.descripcion,
      estado,
    };
  });
}
