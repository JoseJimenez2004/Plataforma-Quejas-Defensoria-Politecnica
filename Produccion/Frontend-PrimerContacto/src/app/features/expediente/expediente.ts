import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteDetalle, EvidenciaDetalle } from '../../core/models/expediente-detalle';
import { NotaAnalisis } from '../../core/models/nota-analisis.model';
import { AcuerdoConciliacion } from '../../core/models/conciliacion.model';
import { CitaPrimerContacto, citaCerrada } from '../../core/models/cita-primer-contacto';
import { ExpedienteService } from '../../core/services/expediente.service';
import { NotaAnalisisService } from '../../core/services/nota-analisis.service';
import { ConciliacionService } from '../../core/services/conciliacion.service';
import { DictamenService } from '../../core/services/dictamen.service';
import { AgendaService } from '../../core/services/agenda.service';
import { AntecedentesService } from '../../core/services/antecedentes.service';
import { AnalistaSesionService } from '../../core/services/analista-sesion.service';
import {
  claseEstatus,
  estaAbierto,
  formatearFechaHora
} from '../../core/utils/estatus-expediente';
import { abrirBlob, mensajeDeError } from '../../core/utils/archivos';

@Component({
  selector: 'app-expediente',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './expediente.html',
  styleUrl: './expediente.css'
})
export class Expediente implements OnInit {
  folio = '';

  nuevaNota = '';

  /** Nota que se está editando en línea (solo notas propias). */
  notaEnEdicion: number | null = null;
  textoEdicion = '';

  expediente: ExpedienteDetalle = {
    folio: '',
    asunto: '',
    fechaIngreso: '',
    estatus: '',
    estatusCodigo: '',
    prioridad: 'Media',
    narrativa: '',
    quejoso: {
      nombre: '',
      boleta: '',
      correo: '',
      telefono: '',
      unidadAcademica: ''
    },
    evidencias: [],
    notas: []
  };

  acuerdos: AcuerdoConciliacion[] = [];
  citaActiva?: CitaPrimerContacto;
  /** Todas las citas del expediente (incluidas las canceladas y reagendadas), más recientes primero. */
  historialCitas: CitaPrimerContacto[] = [];

  /** Resumen de la búsqueda automática de antecedentes al abrir el expediente. */
  antecedentes?: { total: number; mismoQuejoso: number; maxSimilitud: number };

  mostrarFormConciliacion = false;
  conciliacion = { asunto: '', terminos: '' };

  analistaActualId?: number;

  cargando = false;
  sinDatos = false;
  procesando = false;

  readonly claseEstatus = claseEstatus;
  readonly formatearFechaHora = formatearFechaHora;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private expedienteService: ExpedienteService,
    private notaAnalisisService: NotaAnalisisService,
    private conciliacionService: ConciliacionService,
    private dictamenService: DictamenService,
    private agendaService: AgendaService,
    private antecedentesService: AntecedentesService,
    private analistaSesion: AnalistaSesionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.analistaSesion.obtener().subscribe({
      next: (analista) => {
        this.analistaActualId = analista.id;
        this.cdr.detectChanges();
      }
    });

    this.route.paramMap.subscribe(params => {
      this.folio = params.get('id') ?? '';

      if (this.folio) {
        this.cargarExpediente();
      }
    });
  }

  get abierto(): boolean {
    return estaAbierto(this.expediente.estatusCodigo);
  }

  get conciliacionPendiente(): boolean {
    return this.acuerdos.some(a => a.estado === 'PENDIENTE');
  }

  /**
   * CU-PC-03: abrir el expediente lo pasa de TURNADA a EN_ANALISIS (el backend registra
   * quién y cuándo). Por eso aquí se usa iniciarAnalisis y no una consulta simple.
   */
  cargarExpediente(): void {
    this.cargando = true;
    this.sinDatos = false;

    this.expedienteService.iniciarAnalisis(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cargando = false;
        this.cdr.detectChanges();

        this.cargarConciliaciones();
        this.cargarCitaActiva();
        this.cargarResumenAntecedentes();
      },
      error: (error) => {
        this.cargando = false;

        if (error.status === 404) {
          this.sinDatos = true;
          this.cdr.detectChanges();
          this.snackBar.open(
            `No se ha recibido de Revisión ningún expediente con folio ${this.folio}.`,
            'Cerrar',
            { duration: 4000 }
          );
          return;
        }

        this.cdr.detectChanges();
        this.snackBar.open(
          'No fue posible cargar el expediente. Intenta nuevamente o contacta al administrador del sistema.',
          'Cerrar',
          { duration: 4000 }
        );
      }
    });
  }

  /** Recarga sin volver a "abrir" (solo consulta). */
  private recargarExpediente(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cdr.detectChanges();
      }
    });
  }

  // ---------------------------------------------------------------- Evidencias (CU-06/07)

  abrirEvidencia(evidencia: EvidenciaDetalle): void {
    this.expedienteService.descargarEvidencia(evidencia.id).subscribe({
      next: (archivo) => abrirBlob(archivo),
      error: (error) => {
        this.snackBar.open(
          mensajeDeError(error, 'No fue posible abrir la evidencia.'),
          'Cerrar',
          { duration: 3500 }
        );
      }
    });
  }

  // ---------------------------------------------------------------- Notas (CU-05)

  esNotaPropia(nota: NotaAnalisis): boolean {
    return this.analistaActualId !== undefined && nota.analistaId === this.analistaActualId;
  }

  agregarNota(): void {
    const nota = this.nuevaNota.trim();

    if (!nota) return;

    if (!this.expediente.folio) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    this.notaAnalisisService.crearNota({
      folio: this.expediente.folio,
      contenido: nota
    }).subscribe({
      next: (notaGuardada) => {
        this.expediente.notas = [notaGuardada, ...this.expediente.notas];
        this.nuevaNota = '';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible guardar la nota.'), 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  editarNota(nota: NotaAnalisis): void {
    this.notaEnEdicion = nota.id;
    this.textoEdicion = nota.contenido;
  }

  cancelarEdicion(): void {
    this.notaEnEdicion = null;
    this.textoEdicion = '';
  }

  guardarEdicion(nota: NotaAnalisis): void {
    const contenido = this.textoEdicion.trim();
    if (!contenido) return;

    this.notaAnalisisService.actualizarNota(nota.id, {
      folio: this.expediente.folio,
      contenido
    }).subscribe({
      next: (actualizada) => {
        this.expediente.notas = this.expediente.notas.map(n => n.id === actualizada.id ? actualizada : n);
        this.cancelarEdicion();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible editar la nota.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  eliminarNota(nota: NotaAnalisis): void {
    if (!confirm('¿Eliminar esta nota de análisis? Esta acción no se puede deshacer.')) return;

    this.notaAnalisisService.eliminarNota(nota.id).subscribe({
      next: () => {
        this.expediente.notas = this.expediente.notas.filter(n => n.id !== nota.id);
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible eliminar la nota.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  // ---------------------------------------------------------------- Conciliación (CU-10)

  cargarConciliaciones(): void {
    const estabaAbierto = this.abierto;

    this.conciliacionService.listar(this.folio).subscribe({
      next: (acuerdos) => {
        this.acuerdos = acuerdos;
        this.cdr.detectChanges();

        // Si el quejoso ya aceptó, el backend acaba de turnar el expediente.
        if (estabaAbierto && acuerdos.some(a => a.estado === 'ACEPTADO')) {
          this.recargarExpediente();
        }
      }
    });
  }

  proponerConciliacion(): void {
    const asunto = this.conciliacion.asunto.trim();
    const terminos = this.conciliacion.terminos.trim();

    if (!asunto || !terminos) {
      this.snackBar.open('Escribe el asunto y los términos del acuerdo.', 'Cerrar', { duration: 3000 });
      return;
    }

    if (!confirm('El acuerdo se enviará al quejoso para que lo acepte o rechace desde su panel. ¿Continuar?')) {
      return;
    }

    this.procesando = true;

    this.conciliacionService.proponer({ folio: this.folio, asunto, terminos }).subscribe({
      next: (acuerdo) => {
        this.acuerdos = [acuerdo, ...this.acuerdos];
        this.conciliacion = { asunto: '', terminos: '' };
        this.mostrarFormConciliacion = false;
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open('Acuerdo enviado al quejoso.', 'Cerrar', { duration: 3000 });
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible proponer el acuerdo.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }

  etiquetaAcuerdo(estado: AcuerdoConciliacion['estado']): string {
    switch (estado) {
      case 'ACEPTADO':
        return 'Aceptado por el quejoso';
      case 'RECHAZADO':
        return 'Rechazado por el quejoso';
      default:
        return 'Esperando respuesta';
    }
  }

  // ---------------------------------------------------------------- Antecedentes

  /** Al abrir el expediente se busca en automático; el detalle está en su propia pantalla. */
  private cargarResumenAntecedentes(): void {
    this.antecedentesService.buscar(this.folio).subscribe({
      next: (busqueda) => {
        this.antecedentes = {
          total: busqueda.resultados.length,
          mismoQuejoso: busqueda.resultados.filter(a => a.mismoQuejoso).length,
          maxSimilitud: Math.max(0, ...busqueda.resultados.map(a => a.similitud))
        };
        this.cdr.detectChanges();
      }
    });
  }

  buscarAntecedentes(): void {
    this.router.navigate(['/expediente', this.folio, 'antecedentes']);
  }

  // ---------------------------------------------------------------- Citas (CU-04)

  private cargarCitaActiva(): void {
    this.agendaService.listarPorFolio(this.folio).subscribe({
      next: (citas) => {
        this.citaActiva = citas.find(c => !citaCerrada(c));
        this.historialCitas = citas;
        this.cdr.detectChanges();
      }
    });
  }

  agendarCita(): void {
    this.router.navigate(['/agenda'], {
      state: {
        folio: this.expediente.folio
      }
    });
  }

  // ---------------------------------------------------------------- Dictamen / remisión

  marcarCompetente(): void {
    this.router.navigate(['/dictamen', this.expediente.folio]);
  }

  marcarImprocedente(): void {
    this.router.navigate(
      ['/dictamen', this.expediente.folio],
      {
        queryParams: {
          tipo: 'improcedente'
        }
      }
    );
  }

  verDictamen(): void {
    this.router.navigate(['/dictamen', this.expediente.folio, 'consulta']);
  }

  irARemision(): void {
    this.router.navigate(['/remision', this.expediente.folio]);
  }

  reenviarASubdefensoria(): void {
    this.procesando = true;

    this.dictamenService.reenviarASubdefensoria(this.folio).subscribe({
      next: (dictamen) => {
        this.procesando = false;
        this.expediente.folioSubdefensoria = dictamen.folioSubdefensoria;
        this.cdr.detectChanges();
        this.snackBar.open(
          `Subdefensoría recibió el expediente (${dictamen.folioSubdefensoria}).`,
          'Cerrar',
          { duration: 3500 }
        );
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible reenviar a Subdefensoría.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }
}
