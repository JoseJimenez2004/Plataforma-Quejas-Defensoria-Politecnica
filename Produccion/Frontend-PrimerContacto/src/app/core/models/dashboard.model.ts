import { PrioridadEtiqueta } from '../utils/estatus-expediente';

export interface DashboardResumen {
  titulo: string;
  valor: number;
  icono: string;
  tipo: 'PENDIENTES' | 'CON_CITA' | 'EN_DICTAMEN' | 'REMITIDOS';
}

export interface DashboardActividad {
  folio: string;
  accion: string;
  tiempo: string;
  icono: string;
  prioridad: PrioridadEtiqueta;
}

export interface DashboardCitaHoy {
  hora: string;
  quejoso: string;
  folio: string;
  tipo: 'Presencial' | 'Virtual';
  /** Estado real de la cita ('Confirmada'...) y su código (CONFIRMADA...) para el color. */
  estado?: string;
  estadoCodigo?: string;
}

export interface DashboardItemLista {
  folio: string;
  nombre: string;
  detalle: string;
  estado?: string;
  estadoCodigo?: string;
  unidad?: string;
  /** dd/mm/aaaa en que llegó a Primer Contacto. */
  fecha?: string;
  tipo: 'PENDIENTES' | 'CON_CITA' | 'EN_DICTAMEN' | 'REMITIDOS';
}