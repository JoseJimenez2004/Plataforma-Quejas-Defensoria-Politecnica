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
 * Códigos de estatus que realmente emite el backend.
 *
 * OJO: antes esta lista decía RECIBIDA / EN_REVISION / FINALIZADA, que no existen en el
 * backend salvo la primera. Como el traductor caía en `default: 'Recibida'`, una queja
 * RECHAZADA o TURNADA se le mostraba al quejoso como "Recibida" — es decir, el panel le
 * mentía sobre el estado de su trámite. Estos son los códigos reales, tomados de
 * RevisionQuejaService y QuejaService.
 */
export type CodigoEstatus =
  | 'RECIBIDA'
  | 'EN_VALIDACION'
  | 'TURNADA'
  | 'RECHAZADA'
  | 'CANCELADA';

/** Etiqueta legible de cada estatus. */
export type EstatusQueja =
  | 'Recibida'
  | 'En validación'
  | 'Turnada'
  | 'Rechazada'
  | 'Corregida'
  | 'Cancelada';

export function etiquetaEstatus(estatus: string | null | undefined): EstatusQueja {
  switch (estatus) {
    case 'EN_VALIDACION':
      return 'En validación';
    case 'TURNADA':
      return 'Turnada';
    case 'RECHAZADA':
      return 'Rechazada';
    case 'CORREGIDA':
      return 'Corregida';
    case 'CANCELADA':
      return 'Cancelada';
    case 'RECIBIDA':
    default:
      // Las quejas creadas antes de que existiera la columna traen null: se tratan como
      // recibidas, que es el estado en el que nacen.
      return 'Recibida';
  }
}

/** Clase CSS de la insignia de estatus — separa el color del texto. */
export function claseEstatus(estatus: string | null | undefined): string {
  switch (estatus) {
    case 'EN_VALIDACION':
      return 'badge--validacion';
    case 'TURNADA':
      return 'badge--turnada';
    case 'RECHAZADA':
      return 'badge--rechazada';
    case 'CORREGIDA':
      return 'badge--validacion';
    case 'CANCELADA':
      return 'badge--cancelada';
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
 * que todavía se sale.
 */
export function estaCerrada(estatus: string | null | undefined): boolean {
  return estatus === 'CANCELADA';
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

/**
 * Arma la línea del tiempo que ve el quejoso.
 *
 * Son cuatro pasos porque son los cuatro que el sistema puede comprobar hoy con un dato real:
 * el estatus de la queja. Poner los seis pasos "completos" del proceso institucional se veía
 * mejor, pero tres de ellos no tendrían de dónde encenderse y la barra se quedaría clavada
 * para siempre, que es peor que mostrar menos.
 *
 * RECHAZADA y CANCELADA no son un paso más: son finales alternativos, y por eso cortan la
 * línea en vez de avanzarla.
 */
export function pasosDelTramite(estatus: string | null | undefined): PasoTramite[] {
  const codigo = estatus ?? 'RECIBIDA';

  const base: Omit<PasoTramite, 'estado'>[] = [
    {
      clave: 'RECIBIDA',
      titulo: 'Queja recibida',
      descripcion: 'Tu queja se registró y tiene folio.',
    },
    {
      clave: 'EN_VALIDACION',
      titulo: 'En validación',
      descripcion: 'La Defensoría revisa que la documentación esté completa.',
    },
    {
      clave: 'TURNADA',
      titulo: 'Turnada al área',
      descripcion: 'Se asignó al área que dará seguimiento a tu caso.',
    },
    {
      clave: 'ATENDIDA',
      titulo: 'En atención',
      descripcion: 'El área designada trabaja en tu asunto.',
    },
  ];

  if (codigo === 'RECHAZADA' || codigo === 'CANCELADA') {
    const cancelado = codigo === 'CANCELADA';
    return [
      { ...base[0], estado: 'completado' },
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

  const orden = ['RECIBIDA', 'EN_VALIDACION', 'TURNADA'];
  const actual = orden.indexOf(codigo);

  return base.map((paso, i) => ({
    ...paso,
    estado: i < actual ? 'completado' : i === actual ? 'actual' : 'pendiente',
  }));
}
