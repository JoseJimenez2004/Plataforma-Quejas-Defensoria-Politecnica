export type RolStaff =
  | 'ADMIN_SISTEMAS'
  | 'RECEPCIONISTA'
  | 'ANALISTA_PRIMER_CONTACTO'
  | 'SUBDEFENSOR'
  | 'DEFENSOR';

export function etiquetaRol(rol: RolStaff | string): string {
  switch (rol) {
    case 'ADMIN_SISTEMAS':
      return 'Admin. de Sistemas';
    case 'RECEPCIONISTA':
      return 'Recepcionista';
    case 'ANALISTA_PRIMER_CONTACTO':
      return 'Analista de Primer Contacto';
    case 'SUBDEFENSOR':
      return 'Subdefensor';
    case 'DEFENSOR':
      return 'Defensor';
    default:
      return rol;
  }
}

/** Iniciales para el avatar circular del topbar. */
export function iniciales(nombreCompleto: string): string {
  const partes = (nombreCompleto ?? '').trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return '?';
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
  return (partes[0][0] + partes[1][0]).toUpperCase();
}

export interface LoginRequest {
  correo: string;
  password: string;
}

/** Login lo emite admin-service (POST /api/admin/auth/login) -- este front no tiene su
 * propio backend de autenticación, reutiliza los accesos que Admin de Sistemas crea. */
export interface AuthResponse {
  token: string;
  nombre: string;
  rol: RolStaff;
  forzarCambioPassword: boolean;
}

export interface EvidenciaResumen {
  id: number;
  nombreArchivo: string;
  tipoMime: string;
  tamanioBytes: number;
  fechaSubida: string;
}

/** Filtros de la Bandeja de Entrada (ver revision-service BandejaController). */
export type FiltroBandeja = 'todas' | 'disponibles' | 'mias' | 'corregidas' | 'en_atencion';

export interface QuejaResumenBandeja {
  numeroFolio: string;
  fechaCreacion: string;
  nombreQuejoso: string;
  unidadAcademicaClave?: string | null;
  documentacionAparenteCompleta: boolean;
  /** RECIBIDA (nueva), CORREGIDA (reenviada tras un rechazo) o EN_VALIDACION (alguien la atiende). */
  estatus: string;
  /** Quién la tiene abierta; solo cuando estatus es EN_VALIDACION. */
  revisandoPorNombre?: string | null;
  /** true si quien consulta es quien la tiene abierta. */
  esMia: boolean;
  /** WEB (formulario público) o MANUAL (capturada en ventanilla). */
  origen: 'WEB' | 'MANUAL';
  /** En una corregida: quién la había regresado al quejoso. */
  rechazadaAntesPor?: string | null;
}

export interface IndicadoresBandeja {
  porAtender: number;
  nuevasHoy: number;
  corregidas: number;
  enAtencion: number;
  enAtencionMias: number;
  atendidasHoy: number;
  atendidasHoyMias: number;
  turnadasHoy: number;
  rechazadasHoy: number;
  folioMasAntiguo?: string | null;
  fechaMasAntigua?: string | null;
  /** Horas promedio entre que llega una queja y Recepción la valida (últimos 30 días). */
  horasPromedioAtencion?: number | null;
  atendidasUltimos30: number;
}

export interface CargaRecepcionista {
  nombre: string;
  soyYo: boolean;
  enAtencion: number;
  atendidasHoy: number;
}

export interface BandejaResumen {
  indicadores: IndicadoresBandeja;
  lista: QuejaResumenBandeja[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  equipo: CargaRecepcionista[];
}

export interface QuejaDetalle {
  numeroFolio: string;
  fechaCreacion: string;
  nombreCompletoQuejoso: string;
  correoInstitucional: string;
  telefonoContacto: string | null;
  tipoIdentificacionQuejoso: string | null;
  numeroIdentificacionQuejoso: string | null;
  motivo: string;
  descripcion: string;
  unidadAcademicaClave: string | null;
  fechaHechos: string | null;
  nombreCompletoDenunciado: string;
  origenRegistro: string;
  estatus: string;
  evidencias: EvidenciaResumen[];
  motivoRechazo: string | null;
  areaTurnada: string | null;
  defensorAsignado: string | null;
  comentariosRecepcion: string | null;
}

export interface AntecedenteItem {
  numeroFolio: string;
  fecha: string;
  asunto: string;
  estadoActual: string;
  /** MISMA_PERSONA (misma persona quejosa) o MISMO_DENUNCIADO (misma persona señalada). */
  relacion: 'MISMA_PERSONA' | 'MISMO_DENUNCIADO';
  unidadAcademicaClave?: string | null;
  defensorAsignado?: string | null;
  enCurso: boolean;
}

/** Abogado con su carga de trabajo, para decidir a quién turnar. */
export interface CargaAbogado {
  id: number;
  nombre: string;
  rol: string;
  cargaActiva: number;
  totalAsignadas: number;
  foliosAntecedente: string[];
  sugerido: boolean;
  motivoSugerencia?: string | null;
}

export interface RechazarQuejaRequest {
  motivos: string[];
  observaciones: string;
}

export interface TurnarQuejaRequest {
  defensorAsignado: string;
  comentarios: string;
}

export interface AreaOpcion {
  clave: string;
  nombre: string;
}

export interface DefensorOpcion {
  id: number;
  nombreCompleto: string;
  rol: string;
}

export interface IdentificacionOficialOpcion {
  clave: string;
  nombre: string;
  /** Identificación oficial, Identificación institucional (IPN) o Representación. */
  grupo: string;
  detalle?: string;
}

export interface RegistroManualResponse {
  numeroFolio: string;
  mensaje: string;
}

export interface HistorialItem {
  numeroFolio: string;
  fechaCreacion: string;
  nombreQuejoso: string;
  tipoIngreso: string;
  estatusFinal: string;
  motivoRechazo: string | null;
}

/** Historial paginado (2026-10-16). */
export interface HistorialFila {
  numeroFolio: string;
  fechaCreacion: string;
  fechaAtencion?: string | null;
  horasAtencion?: number | null;
  nombreQuejoso: string;
  unidadAcademicaClave?: string | null;
  /** Web | Ventanilla */
  tipoIngreso: string;
  /** TURNADO | RECHAZADO */
  resultado: 'TURNADO' | 'RECHAZADO';
  estatusActual: string;
  atendioNombre?: string | null;
  esMia: boolean;
  folioPrimerContacto?: string | null;
  defensorAsignado?: string | null;
}

export interface HistorialResumen {
  total: number;
  turnadas: number;
  rechazadas: number;
  atendidasPorMi: number;
  horasPromedioAtencion?: number | null;
}

export interface HistorialPagina {
  resumen: HistorialResumen;
  filas: HistorialFila[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}
