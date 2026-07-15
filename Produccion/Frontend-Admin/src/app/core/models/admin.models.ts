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
