import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

import { AlertaVencimiento } from '../../core/models/alerta-vencimiento';
import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { AlertaService } from '../../core/services/alerta.service';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { AccionesExpedienteService } from '../../core/services/acciones-expediente.service';
import { colorPlazo, diasRestantes, formatearFecha, porVencer, porcentajePlazo } from '../../core/utils/etapas';

type Pestana = 'vencidos' | 'por-vencer' | 'espera';

/** Plazos y oficios: "¿Quién no me ha contestado?" */
@Component({
  selector: 'app-alertas',
  imports: [MatDialogModule, MatSnackBarModule],
  templateUrl: './alertas.html',
  styleUrl: './alertas.css'
})
export class Alertas implements OnInit {
  readonly formatearFecha = formatearFecha;
  readonly diasRestantes = diasRestantes;
  readonly porcentajePlazo = porcentajePlazo;
  readonly colorPlazo = colorPlazo;

  pestana: Pestana = 'vencidos';
  cargando = true;

  vencidos: AlertaVencimiento[] = [];
  enEspera: ExpedienteResumen[] = [];
  private porFolio = new Map<string, ExpedienteResumen>();

  constructor(
    private alertaService: AlertaService,
    private expedienteResumenService: ExpedienteResumenService,
    private acciones: AccionesExpedienteService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    forkJoin({
      vencidos: this.alertaService.obtenerVencidos(),
      expedientes: this.expedienteResumenService.listarTodos()
    }).subscribe({
      next: ({ vencidos, expedientes }) => {
        this.vencidos = [...vencidos].sort((a, b) => b.diasRetraso - a.diasRetraso);
        this.porFolio = new Map(expedientes.map(e => [e.folio, e]));
        this.enEspera = expedientes
          .filter(e => e.estatusOficioVigente === 'EN_ESPERA')
          .sort((a, b) => (diasRestantes(a) ?? 0) - (diasRestantes(b) ?? 0));
        if (!this.vencidos.length && this.porVencer.length) this.pestana = 'por-vencer';
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar los plazos.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  get porVencer(): ExpedienteResumen[] {
    return this.enEspera.filter(porVencer);
  }

  get listaEspera(): ExpedienteResumen[] {
    return this.pestana === 'por-vencer' ? this.porVencer : this.enEspera;
  }

  expedienteDe(a: AlertaVencimiento): ExpedienteResumen | undefined {
    return this.porFolio.get(a.folio);
  }

  recordatorio(e: ExpedienteResumen | undefined): void {
    if (e) this.acciones.ejecutar(e, 'recordatorio').subscribe(c => { if (c) this.cargar(); });
  }

  respuesta(e: ExpedienteResumen | undefined): void {
    if (e) this.acciones.ejecutar(e, 'respuesta').subscribe(c => { if (c) this.cargar(); });
  }

  abrir(e: ExpedienteResumen | undefined): void {
    if (e) this.acciones.abrir(e);
  }
}
