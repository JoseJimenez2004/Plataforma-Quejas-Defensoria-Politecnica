import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  DashboardActividad,
  DashboardCitaHoy,
  DashboardItemLista,
  DashboardResumen
} from '../models/dashboard.model';

import { BandejaService } from './bandeja.service';
import { AgendaService } from './agenda.service';
import { ExpedienteBandeja } from '../models/expediente-bandeja';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private bandejaService: BandejaService,
    private agendaService: AgendaService
  ) {}

  obtenerResumen(): Observable<DashboardResumen[]> {
    return this.bandejaService.obtenerBandeja().pipe(
      map(expedientes => this.construirResumen(expedientes))
    );
  }

  obtenerLista(): Observable<DashboardItemLista[]> {
    return this.bandejaService.obtenerBandeja().pipe(
      map(expedientes => expedientes.map(exp => this.mapearItemLista(exp)))
    );
  }

  obtenerCitasHoy(): Observable<DashboardCitaHoy[]> {
    return this.agendaService.obtenerAgendaDia(this.formatearFechaHoy()).pipe(
      map(citas => citas
        .filter(cita => cita.estatus !== 'Cancelada')
        .map(cita => ({
          hora: cita.hora,
          quejoso: cita.quejoso,
          folio: cita.folio,
          tipo: cita.tipo
        })))
    );
  }

  obtenerActividad(): Observable<DashboardActividad[]> {
    return this.bandejaService.obtenerBandeja().pipe(
      map(expedientes => this.construirActividad(expedientes))
    );
  }

  private construirResumen(
    expedientes: ExpedienteBandeja[]
  ): DashboardResumen[] {
    const contar = (tipo: DashboardResumen['tipo']) =>
      expedientes.filter(e => this.tipoDeExpediente(e) === tipo).length;

    return [
      { titulo: 'Pendientes', valor: contar('PENDIENTES'), icono: 'assignment', tipo: 'PENDIENTES' },
      { titulo: 'Con cita', valor: contar('CON_CITA'), icono: 'event', tipo: 'CON_CITA' },
      { titulo: 'Dictaminados', valor: contar('EN_DICTAMEN'), icono: 'description', tipo: 'EN_DICTAMEN' },
      { titulo: 'Remitidos', valor: contar('REMITIDOS'), icono: 'outgoing_mail', tipo: 'REMITIDOS' }
    ];
  }

  private mapearItemLista(
    exp: ExpedienteBandeja
  ): DashboardItemLista {
    return {
      folio: exp.folio,
      nombre: exp.nombreQuejoso,
      detalle: exp.tema,
      estado: exp.estatus,
      tipo: this.tipoDeExpediente(exp)
    };
  }

  private construirActividad(expedientes: ExpedienteBandeja[]): DashboardActividad[] {
    return expedientes
      .slice()
      .sort((a, b) => this.aTimestamp(b.fechaRecepcion) - this.aTimestamp(a.fechaRecepcion))
      .slice(0, 5)
      .map(exp => ({
        folio: exp.folio,
        accion: `Estatus actual: ${exp.estatus}`,
        tiempo: exp.fechaRecepcion,
        icono: 'assignment',
        prioridad: exp.prioridad
      }));
  }

  /**
   * Una sola regla, la misma que la bandeja: "con cita" es el indicador tieneCitaActiva
   * que calcula el backend (antes el dashboard contaba solo las citas de HOY, y la bandeja
   * cualquier cita activa: dos definiciones de lo mismo).
   */
  private tipoDeExpediente(
    exp: ExpedienteBandeja
  ): DashboardResumen['tipo'] {
    switch (exp.estatusCodigo) {
      case 'PROCEDENTE':
      case 'IMPROCEDENTE':
        return 'EN_DICTAMEN';
      case 'REMITIDA':
        return 'REMITIDOS';
      default:
        return exp.tieneCitaActiva ? 'CON_CITA' : 'PENDIENTES';
    }
  }

  private aTimestamp(fecha: string): number {
    if (!fecha) return 0;
    const [day, month, year] = fecha.split('/');
    return new Date(+year, +month - 1, +day).getTime();
  }

  private formatearFechaHoy(): string {
    const hoy = new Date();
    const y = hoy.getFullYear();
    const m = (hoy.getMonth() + 1).toString().padStart(2, '0');
    const d = hoy.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
