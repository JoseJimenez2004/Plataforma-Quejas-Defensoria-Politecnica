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
  prioridad: 'Alta' | 'Media' | 'Baja';
}

export interface DashboardCitaHoy {
  hora: string;
  quejoso: string;
  folio: string;
  tipo: 'Presencial' | 'Virtual';
}

export interface DashboardItemLista {
  folio: string;
  nombre: string;
  detalle: string;
  estado: string;
  tipo: 'PENDIENTES' | 'CON_CITA' | 'EN_DICTAMEN' | 'REMITIDOS';
}