import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { SesionPersonalService } from '../../core/services/sesion-personal.service';
import { AccionesExpedienteService } from '../../core/services/acciones-expediente.service';
import {
  GrupoExpediente, ORDEN_NIVEL, SiguientePaso, diasRestantes, oficioVencido, porVencer, siguientePaso
} from '../../core/utils/etapas';

interface Pendiente {
  e: ExpedienteResumen;
  paso: SiguientePaso;
}

interface Etapa {
  etiqueta: string;
  total: number;
  color: string;
}

/** Inicio: "¿Qué tengo que hacer hoy?" */
@Component({
  selector: 'app-dashboard',
  imports: [MatDialogModule, MatSnackBarModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  fechaHoy = new Date().toLocaleDateString('es-MX', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  saludo = new Date().getHours() < 12 ? 'Buenos días' : new Date().getHours() < 19 ? 'Buenas tardes' : 'Buenas noches';
  nombre = '';

  cargando = true;
  todos: ExpedienteResumen[] = [];
  pendientes: Pendiente[] = [];
  vencenPronto: ExpedienteResumen[] = [];
  etapas: Etapa[] = [];

  enTramite = 0;
  porAbrir = 0;
  porOficio = 0;
  esperando = 0;
  esperandoPorVencer = 0;
  porAcuerdo = 0;
  vencidos = 0;

  readonly diasRestantes = diasRestantes;

  constructor(
    private expedienteResumenService: ExpedienteResumenService,
    private acciones: AccionesExpedienteService,
    private sesion: SesionPersonalService,
    private snackBar: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.nombre = (this.sesion.getNombre() ?? '').split(' ')[0];
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.expedienteResumenService.listarTodos().subscribe({
      next: (items) => {
        this.todos = items;
        this.calcular();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar tus expedientes.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  private calcular(): void {
    const activos = this.todos.filter(e => e.estatus !== 'CONCLUIDO');

    this.enTramite = activos.length;
    this.porAbrir = activos.filter(e => e.estatus === 'RECIBIDO').length;
    this.porOficio = activos.filter(e => !!e.siguienteFase).length;
    this.esperando = activos.filter(e => e.estatus === 'EN_ESPERA_OFICIO' && !oficioVencido(e)).length;
    this.esperandoPorVencer = activos.filter(porVencer).length;
    this.porAcuerdo = activos.filter(e => e.estatus === 'ELABORO_ACUERDO' || e.estatus === 'PENDIENTE_CONCLUSION').length;
    this.vencidos = activos.filter(oficioVencido).length;

    this.pendientes = activos
      .map(e => ({ e, paso: siguientePaso(e) }))
      .filter(p => p.paso.nivel !== 'espera')
      .sort((a, b) => ORDEN_NIVEL[a.paso.nivel] - ORDEN_NIVEL[b.paso.nivel]
        || (a.e.fechaAdmision ?? '').localeCompare(b.e.fechaAdmision ?? ''))
      .slice(0, 8);

    this.vencenPronto = activos
      .filter(e => e.estatusOficioVigente === 'EN_ESPERA' && diasRestantes(e) !== null)
      .sort((a, b) => (diasRestantes(a) ?? 0) - (diasRestantes(b) ?? 0))
      .slice(0, 4);

    const cuenta = (estatus: string[]) => activos.filter(e => estatus.includes(e.estatus)).length;
    this.etapas = [
      { etiqueta: 'Recibido', total: cuenta(['RECIBIDO']), color: '#6366f1' },
      { etiqueta: 'Investigación', total: cuenta(['EN_INVESTIGACION']), color: '#0ea5e9' },
      { etiqueta: 'Espera de oficio', total: cuenta(['EN_ESPERA_OFICIO']), color: '#e0912b' },
      { etiqueta: 'Acuerdo', total: cuenta(['ELABORO_ACUERDO', 'PENDIENTE_CONCLUSION']), color: '#a855f7' },
      { etiqueta: 'Pendiente de acuerdo de conciliación', total: cuenta(['PENDIENTE_CONCILIACION']), color: '#2f5ea8' }
    ];
  }

  ancho(etapa: Etapa): number {
    return this.enTramite ? (etapa.total / this.enTramite) * 100 : 0;
  }

  irA(grupo: GrupoExpediente, plazo?: string): void {
    this.router.navigate(['/bandeja'], { queryParams: { grupo, ...(plazo ? { plazo } : {}) } });
  }

  hacer(p: Pendiente): void {
    this.acciones.ejecutar(p.e, p.paso.accion).subscribe(cambio => {
      if (cambio) this.cargar();
    });
  }

  abrir(e: ExpedienteResumen): void {
    this.acciones.abrir(e);
  }
}
