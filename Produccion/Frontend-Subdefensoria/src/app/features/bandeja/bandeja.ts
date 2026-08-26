import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { RedactarOficioDialog, RedactarOficioDialogData } from '../../shared/redactar-oficio-dialog/redactar-oficio-dialog';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';
import { RespuestaExternaDialog, RespuestaExternaDialogData } from '../../shared/respuesta-externa-dialog/respuesta-externa-dialog';

const ETIQUETAS_ESTATUS: Record<string, string> = {
  RECIBIDO: 'Recibido',
  EN_INVESTIGACION: 'En Investigación',
  EN_GESTION_DIRECTOR: 'Gestión con Director',
  LISTO_A_DICTAMINAR: 'Listo a Dictaminar',
  CONCLUIDO: 'Concluido'
};

interface Faceta {
  valor: string;
  etiqueta: string;
  total: number;
}

@Component({
  selector: 'app-bandeja',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatExpansionModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.css'
})
export class Bandeja implements OnInit {
  displayedColumns = ['folio', 'fechaAdmision', 'quejosoNombre', 'asunto', 'unidadAcademica', 'estatus', 'progreso', 'acciones'];

  todos: ExpedienteResumen[] = [];
  filtrados: ExpedienteResumen[] = [];

  busqueda = '';
  orden: 'recientes' | 'antiguos' = 'recientes';

  facetasEstatus: Faceta[] = [];
  facetasAsunto: Faceta[] = [];
  facetasUnidad: Faceta[] = [];

  estatusSeleccionados = new Set<string>();
  asuntosSeleccionados = new Set<string>();
  unidadesSeleccionadas = new Set<string>();

  readonly etiquetasEstatus = ETIQUETAS_ESTATUS;

  constructor(
    private expedienteResumenService: ExpedienteResumenService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.expedienteResumenService.listarTodos().subscribe({
      next: (items) => {
        this.todos = items;
        this.construirFacetas();
        this.aplicarFiltros();
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar la bandeja de expedientes.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  private construirFacetas(): void {
    this.facetasEstatus = Object.keys(ETIQUETAS_ESTATUS).map(valor => ({
      valor,
      etiqueta: ETIQUETAS_ESTATUS[valor],
      total: this.todos.filter(e => e.estatus === valor).length
    }));

    const asuntos = [...new Set(this.todos.map(e => e.asunto).filter(Boolean))].sort();
    this.facetasAsunto = asuntos.map(valor => ({
      valor,
      etiqueta: valor,
      total: this.todos.filter(e => e.asunto === valor).length
    }));

    const unidades = [...new Set(this.todos.map(e => e.unidadAcademica).filter(Boolean))].sort();
    this.facetasUnidad = unidades.map(valor => ({
      valor,
      etiqueta: valor,
      total: this.todos.filter(e => e.unidadAcademica === valor).length
    }));
  }

  toggleFacet(set: Set<string>, valor: string): void {
    if (set.has(valor)) {
      set.delete(valor);
    } else {
      set.add(valor);
    }
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    const texto = this.busqueda.trim().toLowerCase();

    let resultado = this.todos.filter(e => {
      const pasaEstatus = this.estatusSeleccionados.size === 0 || this.estatusSeleccionados.has(e.estatus);
      const pasaAsunto = this.asuntosSeleccionados.size === 0 || this.asuntosSeleccionados.has(e.asunto);
      const pasaUnidad = this.unidadesSeleccionadas.size === 0 || this.unidadesSeleccionadas.has(e.unidadAcademica);
      const pasaTexto = !texto
        || e.folio.toLowerCase().includes(texto)
        || (e.quejosoNombre ?? '').toLowerCase().includes(texto)
        || (e.asunto ?? '').toLowerCase().includes(texto);

      return pasaEstatus && pasaAsunto && pasaUnidad && pasaTexto;
    });

    resultado = resultado.sort((a, b) => {
      const fechaA = new Date(a.fechaAdmision).getTime();
      const fechaB = new Date(b.fechaAdmision).getTime();
      return this.orden === 'recientes' ? fechaB - fechaA : fechaA - fechaB;
    });

    this.filtrados = resultado;
  }

  etiquetaEstatus(estatus: string): string {
    return this.etiquetasEstatus[estatus] ?? estatus;
  }

  etiquetaAccionPrincipal(e: ExpedienteResumen): string | null {
    if (e.estatus === 'RECIBIDO') return 'Solicitar Oficio';
    if (e.estatus === 'EN_GESTION_DIRECTOR' && !e.numeroOficioVigente) return 'Redactar Oficio al Director';
    return null;
  }

  accionPrincipal(e: ExpedienteResumen): void {
    const siguienteFase = e.estatus === 'RECIBIDO' ? 'SOLICITUD_INFORMACION' : 'GESTION_DIRECTOR';

    const data: RedactarOficioDialogData = {
      expedienteId: e.expedienteId,
      folio: e.folio,
      siguienteFase,
      unidadAcademica: e.unidadAcademica
    };

    this.dialog.open(RedactarOficioDialog, { width: '560px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Oficio generado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargar();
      }
    });
  }

  abrirRecordatorio(e: ExpedienteResumen): void {
    if (!e.oficioIdVigente || !e.numeroOficioVigente || !e.faseOficioVigente) return;

    const data: RecordatorioDialogData = {
      oficioId: e.oficioIdVigente,
      numeroOficio: e.numeroOficioVigente,
      fase: e.faseOficioVigente
    };

    this.dialog.open(RecordatorioDialog, { width: '520px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Recordatorio registrado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargar();
      }
    });
  }

  abrirRespuestaExterna(e: ExpedienteResumen): void {
    if (!e.oficioIdVigente || !e.numeroOficioVigente) return;

    const data: RespuestaExternaDialogData = {
      oficioId: e.oficioIdVigente,
      numeroOficio: e.numeroOficioVigente,
      destinatarioNombre: e.destinatarioNombreVigente ?? e.unidadAcademica
    };

    this.dialog.open(RespuestaExternaDialog, { width: '560px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Respuesta registrada correctamente.', 'Cerrar', { duration: 3000 });
        this.cargar();
      }
    });
  }

  verDetalle(e: ExpedienteResumen): void {
    this.router.navigate(['/expediente', e.folio]);
  }
}