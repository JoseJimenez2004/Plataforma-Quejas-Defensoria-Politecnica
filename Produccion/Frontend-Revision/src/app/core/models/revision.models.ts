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

export interface QuejaResumenBandeja {
  numeroFolio: string;
  fechaCreacion: string;
  nombreQuejoso: string;
  documentacionAparenteCompleta: boolean;
  estatus: string;
}

export interface BandejaResumen {
  pendientes: number;
  enProceso: number;
  turnadasHoy: number;
  lista: QuejaResumenBandeja[];
}

export interface QuejaDetalle {
  numeroFolio: string;
  fechaCreacion: string;
  nombreCompletoQuejoso: string;
  correoInstitucional: string;
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
}

export interface RechazarQuejaRequest {
  motivos: string[];
  observaciones: string;
}

export interface TurnarQuejaRequest {
  areaTurnada: string;
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
