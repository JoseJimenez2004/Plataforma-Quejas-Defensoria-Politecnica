import { Injectable } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import {
  DashboardActividad,
  DashboardCitaHoy,
  DashboardItemLista,
  DashboardResumen
} from '../models/dashboard.model';

import { BandejaService } from './bandeja.service';
import { AgendaService } from './agenda.service';
import { ExpedienteBandeja } from '../models/expediente-bandeja';
import { CitaPrimerContacto } from '../models/cita-primer-contacto';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private bandejaService: BandejaService,
    private agendaService: AgendaService
  ) {}

  obtenerResumen(): Observable<DashboardResumen[]> {
    return this.combinarBandejaYCitasHoy().pipe(
      map(({ expedientes, folioConCita }) => this.construirResumen(expedientes, folioConCita))
    );
  }

  obtenerLista(): Observable<DashboardItemLista[]> {
    return this.combinarBandejaYCitasHoy().pipe(
      map(({ expedientes, folioConCita }) =>
        expedientes.map(exp => this.mapearItemLista(exp, folioConCita))
      )
    );
  }

  obtenerCitasHoy(): Observable<DashboardCitaHoy[]> {
    return this.agendaService.obtenerAgendaDia(this.formatearFechaHoy()).pipe(
      map(citas => citas.map(cita => ({
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

  private combinarBandejaYCitasHoy(): Observable<{
    expedientes: ExpedienteBandeja[];
    folioConCita: Set<string>;
  }> {
    return forkJoin({
      expedientes: this.bandejaService.obtenerBandeja(),
      citasHoy: this.agendaService.obtenerAgendaDia(this.formatearFechaHoy())
    }).pipe(
      map(({ expedientes, citasHoy }: { expedientes: ExpedienteBandeja[]; citasHoy: CitaPrimerContacto[] }) => ({
        expedientes,
        folioConCita: new Set(citasHoy.map(c => c.folio))
      }))
    );
  }

  private construirResumen(
    expedientes: ExpedienteBandeja[],
    folioConCita: Set<string>
  ): DashboardResumen[] {
    const contar = (tipo: DashboardResumen['tipo']) =>
      expedientes.filter(e => this.tipoDeExpediente(e, folioConCita) === tipo).length;

    return [
      { titulo: 'Pendientes', valor: contar('PENDIENTES'), icono: 'assignment', tipo: 'PENDIENTES' },
      { titulo: 'Con cita', valor: contar('CON_CITA'), icono: 'event', tipo: 'CON_CITA' },
      { titulo: 'En dictamen', valor: contar('EN_DICTAMEN'), icono: 'description', tipo: 'EN_DICTAMEN' },
      { titulo: 'Remitidos', valor: contar('REMITIDOS'), icono: 'outgoing_mail', tipo: 'REMITIDOS' }
    ];
  }

  private mapearItemLista(
    exp: ExpedienteBandeja,
    folioConCita: Set<string>
  ): DashboardItemLista {
    return {
      folio: exp.folio,
      nombre: exp.nombreQuejoso,
      detalle: exp.tema,
      estado: exp.estatus,
      tipo: this.tipoDeExpediente(exp, folioConCita)
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

  private tipoDeExpediente(
    exp: ExpedienteBandeja,
    folioConCita: Set<string>
  ): DashboardResumen['tipo'] {
    if (folioConCita.has(exp.folio)) {
      return 'CON_CITA';
    }

    switch (exp.estatus) {
      case 'Competente':
        return 'EN_DICTAMEN';
      case 'Remitida':
        return 'REMITIDOS';
      case 'Pendiente':
      case 'En análisis':
      default:
        return 'PENDIENTES';
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
