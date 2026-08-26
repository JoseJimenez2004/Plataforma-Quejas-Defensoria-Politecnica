import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { AlertaService } from '../../core/services/alerta.service';
import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { AlertaVencimiento } from '../../core/models/alerta-vencimiento';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';

type Indicador = 'POR_REDACTAR' | 'EN_CURSO' | 'CONCLUIDOS' | 'VENCIDOS';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatTooltipModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  fechaHoy = new Date().toLocaleDateString('es-MX', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  todos: ExpedienteResumen[] = [];
  urgentes: AlertaVencimiento[] = [];

  indicadorSeleccionado: Indicador = 'POR_REDACTAR';
  listaFiltrada: ExpedienteResumen[] = [];

  constructor(
    private expedienteResumenService: ExpedienteResumenService,
    private alertaService: AlertaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.expedienteResumenService.listarTodos().subscribe(items => {
      this.todos = items;
      this.aplicarFiltro();
      this.cdr.detectChanges();
    });

    this.alertaService.obtenerVencidos().subscribe(items => {
      this.urgentes = items.slice(0, 5);
      this.cdr.detectChanges();
    });
  }

  get totalPorRedactar(): number {
    return this.todos.filter(e => e.estatus === 'RECIBIDO' || (e.estatus === 'EN_GESTION_DIRECTOR' && !e.oficioIdVigente)).length;
  }

  get totalEnCurso(): number {
    return this.todos.filter(e => e.estatus === 'EN_INVESTIGACION' || e.estatus === 'EN_GESTION_DIRECTOR').length;
  }

  get totalConcluidos(): number {
    return this.todos.filter(e => e.estatus === 'CONCLUIDO').length;
  }

  get totalVencidos(): number {
    return this.todos.filter(e => e.estatusOficioVigente === 'VENCIDO').length;
  }

  seleccionarIndicador(indicador: Indicador): void {
    this.indicadorSeleccionado = indicador;
    this.aplicarFiltro();
  }

  aplicarFiltro(): void {
    switch (this.indicadorSeleccionado) {
      case 'POR_REDACTAR':
        this.listaFiltrada = this.todos.filter(e => e.estatus === 'RECIBIDO' || (e.estatus === 'EN_GESTION_DIRECTOR' && !e.oficioIdVigente));
        break;
      case 'EN_CURSO':
        this.listaFiltrada = this.todos.filter(e => e.estatus === 'EN_INVESTIGACION' || e.estatus === 'EN_GESTION_DIRECTOR');
        break;
      case 'CONCLUIDOS':
        this.listaFiltrada = this.todos.filter(e => e.estatus === 'CONCLUIDO');
        break;
      case 'VENCIDOS':
        this.listaFiltrada = this.todos.filter(e => e.estatusOficioVigente === 'VENCIDO');
        break;
    }
  }

  verDetalle(e: ExpedienteResumen): void {
    this.router.navigate(['/expediente', e.folio]);
  }

  atender(alerta: AlertaVencimiento): void {
    const data: RecordatorioDialogData = {
      oficioId: alerta.oficioId,
      numeroOficio: alerta.numeroOficio,
      fase: alerta.fase
    };

    this.dialog.open(RecordatorioDialog, { width: '520px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Recordatorio registrado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargar();
      }
    });
  }
}