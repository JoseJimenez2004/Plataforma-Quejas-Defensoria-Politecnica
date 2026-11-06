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
import { ETIQUETAS_ESTATUS } from '../../core/models/estatus-expediente';
import { clasePrioridad, etiquetaPrioridad, rangoPrioridad } from '../../core/utils/prioridad';

interface Pendiente {
  e: ExpedienteResumen;
  paso: SiguientePaso;
}

interface Etapa {
  estatus: string;
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
  esperandoQuejoso = 0;
  vencidos = 0;

  readonly diasRestantes = diasRestantes;
  readonly clasePrioridad = clasePrioridad;
  readonly etiquetaPrioridad = etiquetaPrioridad;

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
    // SD-2: cada cuadro cuenta exactamente lo que muestra al picarlo.
    this.porOficio = activos.filter(e => e.estatus === 'EN_INVESTIGACION').length;
    this.esperando = activos.filter(e => e.estatus === 'EN_ESPERA_OFICIO').length;
    this.esperandoPorVencer = activos.filter(porVencer).length;
    this.porAcuerdo = activos.filter(e => e.estatus === 'ELABORO_ACUERDO' || e.estatus === 'PENDIENTE_CONCLUSION').length;
    this.esperandoQuejoso = activos.filter(e => e.estatus === 'PENDIENTE_CONCILIACION').length;
    this.vencidos = activos.filter(oficioVencido).length;

    this.pendientes = activos
      .map(e => ({ e, paso: siguientePaso(e) }))
      .filter(p => p.paso.nivel !== 'espera')
      .sort((a, b) => ORDEN_NIVEL[a.paso.nivel] - ORDEN_NIVEL[b.paso.nivel]
        || rangoPrioridad(a.e.prioridad) - rangoPrioridad(b.e.prioridad)
        || (a.e.fechaAdmision ?? '').localeCompare(b.e.fechaAdmision ?? ''))
      .slice(0, 8);

    this.vencenPronto = activos
      .filter(e => e.estatusOficioVigente === 'EN_ESPERA' && diasRestantes(e) !== null)
      .sort((a, b) => (diasRestantes(a) ?? 0) - (diasRestantes(b) ?? 0))
      .slice(0, 4);

    const cuenta = (estatus: string[]) => activos.filter(e => estatus.includes(e.estatus)).length;
    // SD-4: mismos nombres que el chip y las pestañas; conciliación y conclusión por separado.
    const etapa = (estatus: keyof typeof ETIQUETAS_ESTATUS, color: string) =>
      ({ etiqueta: ETIQUETAS_ESTATUS[estatus], total: cuenta([estatus]), color, estatus });
    this.etapas = [
      etapa('RECIBIDO', '#6366f1'),
      etapa('EN_INVESTIGACION', '#0ea5e9'),
      etapa('EN_ESPERA_OFICIO', '#e0912b'),
      etapa('ELABORO_ACUERDO', '#a855f7'),
      etapa('PENDIENTE_CONCILIACION', '#2f5ea8'),
      etapa('PENDIENTE_CONCLUSION', '#c026d3')
    ];
  }

  ancho(etapa: Etapa): number {
    return this.enTramite ? (etapa.total / this.enTramite) * 100 : 0;
  }

  irA(grupo: GrupoExpediente, plazo?: string): void {
    this.router.navigate(['/bandeja'], { queryParams: { grupo: grupo === 'tramite' ? null : grupo, ...(plazo ? { plazo } : {}) } });
  }

  /** SD-2: cuadros y barra de etapas llevan a la etapa exacta (?etapa=A,B). */
  irAEtapa(...estatus: string[]): void {
    this.router.navigate(['/bandeja'], { queryParams: { etapa: estatus.join(',') } });
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
