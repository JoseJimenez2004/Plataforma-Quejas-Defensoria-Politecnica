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

export interface LoginAdminRequest {
  correo: string;
  password: string;
}

export interface AuthAdminResponse {
  token: string;
  nombre: string;
  rol: RolStaff;
  forzarCambioPassword: boolean;
}

export interface PersonalResumen {
  id: number;
  nombreCompleto: string;
  numeroEmpleado: string;
  correoInstitucional: string;
  rol: RolStaff;
  activo: boolean;
  cuentaTemporal: boolean;
}

export interface PersonalRequest {
  nombreCompleto?: string;
  numeroEmpleado?: string;
  correoInstitucional?: string;
  rol?: RolStaff;
  passwordTemporal?: string;
  restablecerPassword?: boolean;
  desactivarTemporalmente?: boolean;
}

export interface PersonalCreadoResponse {
  id: number;
  nombreCompleto: string;
  correoInstitucional: string;
  passwordTemporal: string;
}

export interface ResetPasswordResponse {
  passwordTemporalNueva: string;
}

export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}

/** Iniciales para el avatar circular del topbar (ej. "Juan Pérez López" -> "JP"). */
export function iniciales(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return '?';
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
  return (partes[0][0] + partes[1][0]).toUpperCase();
}

export interface DashboardResumen {
  totalPersonalActivo: number;
  totalDependencias: number;
  ultimoRespaldo: string;
  totalPlantillasActivas: number;
  /** null si el backend no pudo calcularlos (p. ej. la tabla quejas aún no existe). */
  quejas: IndicadoresQuejas | null;
}

export interface ConteoIndicador {
  clave: string;
  etiqueta: string;
  total: number;
}

/** Indicadores de quejas del dashboard de la Defensora (admin-service). */
export interface IndicadoresQuejas {
  total: number;
  recibidas: number;
  enValidacion: number;
  corregidas: number;
  turnadas: number;
  rechazadas: number;
  canceladas: number;
  ultimos30Dias: number;
  turnadasEsteMes: number;
  respuestasDenunciado: number;
  unidadesConMasQuejas: ConteoIndicador[];
  porMes: ConteoIndicador[];
}

/** Pregunta del chatbot del portal (chatbot-service). */
export interface PreguntaChatbot {
  id: number;
  categoria: string;
  pregunta: string;
  respuesta: string;
  orden: number;
  activo: boolean;
}

export interface PreguntaChatbotRequest {
  categoria: string;
  pregunta: string;
  respuesta: string;
  orden: number;
  activo: boolean;
}

export interface PlantillaDocumento {
  id: number;
  tipo: string;
  nombre: string;
  contenido: string;
  activa: boolean;
  actualizadoEn: string;
  actualizadoPor?: string;
}

export interface RespaldoResumen {
  nombreArchivo: string;
  tamanioBytes: number;
  fecha: string;
}

export interface BitacoraAccion {
  id: number;
  usuario: string;
  accionRealizada: string;
  ip: string;
  fecha: string;
}

export interface Dependencia {
  id: number;
  clave: string;
  clavePadre?: string;
  nombre: string;
  abreviatura?: string;
  tipo: string;
  categoria?: string;
  nivel: number;
  activo: boolean;
  correoContacto?: string;
  nombreTitular?: string;
}

// ======================================================================================
// Seguimiento de quejas (Panel de la Defensora, 2026-10-16)
// ======================================================================================

export interface ConteoSeg {
  clave: string;
  etiqueta: string;
  total: number;
}

export interface EtapaSeg {
  clave: string;
  etiqueta: string;
  total: number;
  estatus: ConteoSeg[];
}

export interface MesSeg {
  clave: string;
  etiqueta: string;
  recibidas: number;
  atendidas: number;
}

export interface UnidadSeg {
  clave: string;
  nombre?: string | null;
  abreviatura?: string | null;
  tipo?: string | null;
  total: number;
  enTramite: number;
  cerradas: number;
  ultimaQueja?: string | null;
}

export interface FilaQuejaSeg {
  folio: string;
  fecha: string;
  quejoso: string;
  unidadClave?: string | null;
  unidadNombre?: string | null;
  etapa: string;
  estatus: string;
  estatusEtiqueta: string;
  recepcionista?: string | null;
  atendiendoAhora: boolean;
  abogado?: string | null;
  folioPrimerContacto?: string | null;
  origen: string;
  estado: string;
  estadoEtiqueta: string;
  prioridad?: string | null;
  dias: number;
}

export interface PaginaQuejasSeg {
  filas: FilaQuejaSeg[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface FiltrosSeg {
  recepcionistas: { correo: string; nombre: string }[];
  abogados: string[];
  etapas: ConteoSeg[];
  estados: ConteoSeg[];
}

export interface DetalleQuejaSeg {
  folio: string;
  etapa: string;
  etapaEtiqueta: string;
  estatus: string;
  estatusEtiqueta: string;
  origen: string;
  fechaCreacion?: string | null;
  quejoso?: string | null;
  correo?: string | null;
  telefono?: string | null;
  fechaNacimiento?: string | null;
  tipoIdentificacion?: string | null;
  numeroIdentificacion?: string | null;
  unidadClave?: string | null;
  unidadNombre?: string | null;
  fechaHechos?: string | null;
  denunciado?: string | null;
  descripcion?: string | null;
  recepcionista?: string | null;
  atendiendoAhora: boolean;
  abogado?: string | null;
  folioPrimerContacto?: string | null;
  comentariosRecepcion?: string | null;
  motivoRechazo?: string | null;
  tipoDocumentoFisico?: string | null;
  numeroOficio?: string | null;
  fechaRecepcionFisica?: string | null;
  ubicacionFisica?: string | null;
  evidencias: { id: number; nombre: string; tipoMime?: string | null; tipo?: string | null; tamanio?: number | null; fecha?: string | null }[];
  respuestasDenunciado: number;
  linea: { fecha?: string | null; titulo: string; detalle?: string | null }[];
}

/** Filtros del explorador de quejas (todos opcionales). */
export interface FiltroQuejasSeg {
  etapa?: string;
  estatus?: string;
  unidad?: string;
  abogado?: string;
  recepcionista?: string;
  mes?: string;
  desde?: string;
  hasta?: string;
  estado?: string;
  prioridad?: string;
  motivo?: string;
  orden?: string;
  q?: string;
}

// ======================================================================================
// Panel de la Defensora (diseño 2026-10-18)
// ======================================================================================

export interface PanelKpis {
  total: number;
  recibidasPeriodo: number;
  recibidasAnterior: number;
  enRecepcion: number;
  sinTomarMas3Dias: number;
  conQuejoso: number;
  rechazadasAlgunaVez: number;
  enPrimerContacto: number;
  enSubdefensoria: number;
  cerradas: number;
}

export interface ReglaCaso {
  clave: string;
  etiqueta: string;
  descripcion: string;
  umbralDias: number;
}

export interface CasoAtencion {
  folio: string;
  fecha?: string | null;
  quejoso?: string | null;
  motivo: string;
  motivoEtiqueta: string;
  dias: number;
  severidad: 'ALTA' | 'MEDIA';
  etapa: string;
  estatus: string;
  estatusEtiqueta: string;
  unidadClave?: string | null;
  unidadNombre?: string | null;
  responsable?: string | null;
}

export interface PaginaCasos {
  filas: CasoAtencion[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  reglas: ReglaCaso[];
}

export interface FiltroCasos {
  q?: string;
  motivo?: string;
  etapa?: string;
  unidad?: string;
  responsable?: string;
  diasMin?: number | null;
  diasMax?: number | null;
}

export interface FilaUnidad {
  posicion: number;
  clave: string;
  nombre?: string | null;
  siglas: string;
  tipo: string;
  tipoEtiqueta: string;
  total: number;
  enTramite: number;
  cerradas: number;
  porcentaje: number;
  ultima?: string | null;
}

export interface TablaUnidades {
  filas: FilaUnidad[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  resumen: {
    unidadesConQueja: number;
    unidadesAnterior: number;
    totalQuejas: number;
    totalAnterior: number;
    unidadTop?: string | null;
    unidadTopTotal: number;
    promedio: number;
  };
  porTipo: ConteoSeg[];
  top: FilaUnidad[];
}

export interface FiltroUnidades {
  q?: string;
  tipo?: string;
  estatus?: string;
  desde?: string;
  hasta?: string;
}

export interface MiembroCarga {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
  abiertas: number;
  enTramite: number;
  atendidas: number;
  turnadas: number;
  observaciones: number;
  enPrimerContacto: number;
  enSubdefensoria: number;
  cerradas: number;
  ultimaActividad?: string | null;
  carga: number;
  porcentaje: number;
  nivel: 'ALTA' | 'NORMAL' | 'BAJA' | 'SIN_ACTIVIDAD';
}

export interface CargaTrabajo {
  dias: number;
  kpis: {
    personalActivo: number;
    recepcionistas: number;
    abogados: number;
    enTramite: number;
    totalQuejas: number;
    atendidas: number;
    diasPromedio?: number | null;
    diasPromedioAnterior?: number | null;
  };
  recepcionistas: MiembroCarga[];
  abogados: MiembroCarga[];
  etapas: EtapaSeg[];
}

export interface MotivoRechazo {
  clave: string;
  motivo: string;
  categoria: string;
  cantidad: number;
  porcentaje: number;
}

export interface PanelDefensora {
  dias: number;
  kpis: PanelKpis;
  etapas: EtapaSeg[];
  porMes: MesSeg[];
  unidades: FilaUnidad[];
  atencion: PaginaCasos;
  recepcion: MiembroCarga[];
  motivos: MotivoRechazo[];
}

export interface MiembroPersonal {
  id: number;
  nombre: string;
  numeroEmpleado?: string | null;
  correo: string;
  rol: RolStaff;
  activo: boolean;
  cuentaTemporal: boolean;
  unidadClave?: string | null;
  unidadNombre?: string | null;
  telefono?: string | null;
  fechaRegistro?: string | null;
  ultimoAcceso?: string | null;
  abiertas: number;
  enTramite: number;
  atendidas: number;
  turnadas: number;
  observaciones: number;
}

export interface DetallePersonal {
  persona: MiembroPersonal;
  actividad: { fecha?: string | null; titulo: string; detalle?: string | null }[];
  quejas: { folio: string; fecha?: string | null; quejoso?: string | null; unidad?: string | null; etapa: string; estatusEtiqueta: string; relacion: string }[];
}

/** Periodos del selector "Últimos N días" (0 = todo el historial). */
export const PERIODOS: { dias: number; etiqueta: string }[] = [
  { dias: 7, etiqueta: 'Últimos 7 días' },
  { dias: 30, etiqueta: 'Últimos 30 días' },
  { dias: 90, etiqueta: 'Últimos 90 días' },
  { dias: 365, etiqueta: 'Últimos 12 meses' },
  { dias: 0, etiqueta: 'Todo el historial' },
];

export const ETIQUETA_NIVEL: Record<string, string> = {
  ALTA: 'Alta',
  NORMAL: 'Normal',
  BAJA: 'Baja',
  SIN_ACTIVIDAD: 'Sin actividad',
};

/** Variación porcentual contra el periodo anterior (null si no hay base para comparar). */
export function variacion(actual: number, anterior: number): number | null {
  if (!anterior) {
    return null;
  }
  return Math.round(((actual - anterior) / anterior) * 100);
}

/** Etiqueta en femenino para el titular cuando así lo indica su nombre (Dra., Lic. …a). */
export function etiquetaRolPersona(rol: string, nombre?: string | null): string {
  const n = (nombre ?? '').trim().toLowerCase();
  const femenino = n.startsWith('dra') || n.startsWith('mtra') || /\b(defensora|abogada)\b/.test(n);
  if (rol === 'DEFENSOR') {
    return femenino || n === 'defensora' ? 'Defensora' : 'Defensor';
  }
  return etiquetaRol(rol);
}
