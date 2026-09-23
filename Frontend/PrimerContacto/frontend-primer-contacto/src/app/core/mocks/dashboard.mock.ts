import {
  DashboardActividad,
  DashboardCitaHoy,
  DashboardItemLista,
  DashboardResumen
} from '../models/dashboard.model';

export const DASHBOARD_RESUMEN_MOCK: DashboardResumen[] = [
  { titulo: 'Pendientes', valor: 12, icono: 'assignment', tipo: 'PENDIENTES' },
  { titulo: 'Con cita', valor: 5, icono: 'event', tipo: 'CON_CITA' },
  { titulo: 'En dictamen', valor: 7, icono: 'description', tipo: 'EN_DICTAMEN' },
  { titulo: 'Remitidos', valor: 2, icono: 'outgoing_mail', tipo: 'REMITIDOS' }
];

export const DASHBOARD_ACTIVIDAD_MOCK: DashboardActividad[] = [
  {
    folio: 'DDP-2026-001',
    accion: 'Se agendó cita de primer contacto',
    tiempo: 'Hace 15 minutos',
    icono: 'event',
    prioridad: 'Alta'
  },
  {
    folio: 'DDP-2026-003',
    accion: 'Expediente turnado para análisis',
    tiempo: 'Hace 30 minutos',
    icono: 'assignment',
    prioridad: 'Alta'
  },
  {
    folio: 'DDP-2026-004',
    accion: 'Remisión externa creada',
    tiempo: 'Hace 1 hora',
    icono: 'outgoing_mail',
    prioridad: 'Media'
  }
];

export const DASHBOARD_CITAS_HOY_MOCK: DashboardCitaHoy[] = [
  { hora: '09:00', quejoso: 'Juan Pérez', folio: 'DDP-2026-002', tipo: 'Virtual' },
  { hora: '10:00', quejoso: 'María Fernanda López', folio: 'DDP-2026-001', tipo: 'Presencial' },
  { hora: '12:30', quejoso: 'Carlos Ramírez', folio: 'DDP-2026-004', tipo: 'Presencial' }
];

export const DASHBOARD_LISTA_MOCK: DashboardItemLista[] = [
  { folio: 'DDP-2026-001', nombre: 'María Fernanda López', detalle: 'Presunto acoso', estado: 'Pendiente', tipo: 'PENDIENTES' },
  { folio: 'DDP-2026-003', nombre: 'Ana Torres', detalle: 'Discriminación', estado: 'Pendiente', tipo: 'PENDIENTES' },
  { folio: 'DDP-2026-002', nombre: 'Juan Pérez', detalle: '23/04/2026 · 09:00', estado: 'Programada', tipo: 'CON_CITA' },
  { folio: 'DDP-2026-005', nombre: 'Carlos Ramírez', detalle: 'Dictamen en borrador', estado: 'En dictamen', tipo: 'EN_DICTAMEN' },
  { folio: 'DDP-2026-004', nombre: 'Carlos Ramírez', detalle: 'Remisión externa creada', estado: 'Remitido', tipo: 'REMITIDOS' }
];