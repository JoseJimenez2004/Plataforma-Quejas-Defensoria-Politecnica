import { ChangeDetectorRef, Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AgendaService } from '../../core/services/agenda.service';
import { BandejaService } from '../../core/services/bandeja.service';
import { ExpedienteService } from '../../core/services/expediente.service';
import { AnalistaSesionService } from '../../core/services/analista-sesion.service';
import { ExpedienteBandeja } from '../../core/models/expediente-bandeja';
import {
  CATEGORIAS_CITA,
  CitaPrimerContacto,
  HORARIOS_CITA,
  citaOcupaHorario
} from '../../core/models/cita-primer-contacto';
import { estaAbierto } from '../../core/utils/estatus-expediente';
import { mensajeDeError } from '../../core/utils/archivos';

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre',
  'Octubre', 'Noviembre', 'Diciembre'];

/**
 * "Programar Nueva Cita" (diseño 2026-10-18). Paso 1: expediente, tipo, modalidad y motivo.
 * Paso 2: fecha en el calendario y un horario libre. Si recibe {@link cita}, reagenda esa misma
 * cita (PUT /citas/{id}/reagendar) en vez de crear otra.
 *
 * Un horario está "Ocupado" si la persona analista ya tiene otra cita a esa hora (el backend
 * no permite traslapes), y "No disponible" si ya pasó o cae en fin de semana.
 */
@Component({
  selector: 'app-nueva-cita',
  imports: [FormsModule, MatIconModule, MatSnackBarModule],
  templateUrl: './nueva-cita.html',
  styleUrl: './nueva-cita.css'
})
export class NuevaCita implements OnInit {
  /** Folio PC-... ya elegido (desde el expediente). */
  @Input() folio = '';
  /** Cita a reagendar. */
  @Input() cita?: CitaPrimerContacto;
  /** Fecha sugerida (AAAA-MM-DD), p. ej. el día elegido en el calendario. */
  @Input() fechaInicial = '';
  @Output() guardada = new EventEmitter<CitaPrimerContacto>();
  @Output() cerrar = new EventEmitter<void>();

  readonly categorias = CATEGORIAS_CITA;
  readonly horarios = HORARIOS_CITA;
  readonly diasSemana = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

  expedientes: ExpedienteBandeja[] = [];
  nombreQuejoso = '';
  categoria = 'COMPARECENCIA';
  modalidad: 'PRESENCIAL' | 'VIRTUAL' = 'PRESENCIAL';
  motivo = '';
  fecha = '';
  hora = '';
  guardando = false;

  anio = new Date().getFullYear();
  mes = new Date().getMonth();
  private citasDelDia: CitaPrimerContacto[] = [];
  private analistaId?: number;

  constructor(
    private agenda: AgendaService,
    private bandeja: BandejaService,
    private expedientesService: ExpedienteService,
    private analista: AnalistaSesionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.analista.obtener().subscribe({ next: (a) => { this.analistaId = a.id; this.cdr.detectChanges(); } });

    if (this.cita) {
      this.folio = this.cita.folio;
      this.nombreQuejoso = this.cita.quejoso;
      this.categoria = this.cita.categoria ?? 'COMPARECENCIA';
      this.modalidad = this.cita.tipo === 'Virtual' ? 'VIRTUAL' : 'PRESENCIAL';
      this.motivo = this.cita.motivo;
    } else {
      this.bandeja.obtenerBandeja().subscribe({
        next: (lista) => {
          // Se puede citar mientras el expediente siga abierto y no tenga cita activa.
          this.expedientes = lista.filter(e => estaAbierto(e.estatusCodigo) && (!e.tieneCitaActiva || e.folio === this.folio));
          this.cdr.detectChanges();
        }
      });
      if (this.folio) {
        this.elegirExpediente(this.folio);
      }
    }

    const inicial = this.fechaInicial && this.esHabil(this.fechaInicial) ? this.fechaInicial : this.siguienteDiaHabil();
    this.elegirFecha(inicial);
  }

  get reagendando(): boolean {
    return !!this.cita;
  }

  @HostListener('document:keydown.escape')
  alEscape(): void {
    if (!this.guardando) {
      this.cerrar.emit();
    }
  }

  // ------------------------------------------------------------------ expediente

  elegirExpediente(folio: string): void {
    this.folio = folio;
    const enLista = this.expedientes.find(e => e.folio === folio);
    this.nombreQuejoso = enLista?.nombreQuejoso ?? '';
    this.expedientesService.obtenerPorFolio(folio).subscribe({
      next: (e) => {
        this.nombreQuejoso = e.quejoso?.nombreCompleto ?? this.nombreQuejoso;
        this.cdr.detectChanges();
      }
    });
  }

  // ------------------------------------------------------------------ calendario

  get hoyIso(): string {
    return iso(new Date());
  }

  get tituloMes(): string {
    return `${MESES[this.mes]} ${this.anio}`;
  }

  get celdas(): (number | null)[] {
    const primero = new Date(this.anio, this.mes, 1).getDay();
    const dias = new Date(this.anio, this.mes + 1, 0).getDate();
    return [...Array(primero).fill(null), ...Array.from({ length: dias }, (_, i) => i + 1)];
  }

  isoDe(dia: number): string {
    return iso(new Date(this.anio, this.mes, dia));
  }

  diaInhabil(dia: number): boolean {
    const d = new Date(this.anio, this.mes, dia);
    return d.getDay() === 0 || d.getDay() === 6 || this.isoDe(dia) < this.hoyIso;
  }

  mover(delta: number): void {
    const d = new Date(this.anio, this.mes + delta, 1);
    this.anio = d.getFullYear();
    this.mes = d.getMonth();
  }

  elegirFecha(fecha: string): void {
    if (!fecha) return;
    this.fecha = fecha;
    const [a, m] = fecha.split('-').map(Number);
    this.anio = a;
    this.mes = m - 1;
    this.hora = this.estadoHorario(this.hora) === 'libre' ? this.hora : '';
    this.agenda.obtenerAgendaDia(fecha).subscribe({
      next: (citas) => {
        this.citasDelDia = citas;
        if (this.estadoHorario(this.hora) !== 'libre') {
          this.hora = '';
        }
        this.cdr.detectChanges();
      },
      error: () => { this.citasDelDia = []; this.cdr.detectChanges(); }
    });
  }

  estadoHorario(h: string): 'libre' | 'ocupado' | 'no-disponible' {
    if (!h || !this.fecha) return 'no-disponible';
    const d = new Date(this.fecha + 'T' + h + ':00');
    if (d.getDay() === 0 || d.getDay() === 6 || d.getTime() <= Date.now()) {
      return 'no-disponible';
    }
    const ocupado = this.citasDelDia.some(c =>
      c.hora === h && citaOcupaHorario(c) && c.id !== this.cita?.id
      && (this.analistaId === undefined || c.analistaId === this.analistaId));
    return ocupado ? 'ocupado' : 'libre';
  }

  // ------------------------------------------------------------------ guardar

  guardar(): void {
    if (!this.folio || !this.fecha || !this.hora || !this.motivo.trim()) {
      this.snackBar.open('Completa el expediente, el motivo, la fecha y el horario.', 'Cerrar', { duration: 3000 });
      return;
    }
    this.guardando = true;
    const operacion = this.cita?.id
      ? this.agenda.reagendarCita(this.cita.id, {
          fechaCita: this.fecha, horaCita: this.hora, tipoCita: this.modalidad, categoria: this.categoria, motivo: this.motivo.trim()
        })
      : this.agenda.crearCita({
          folio: this.folio, quejosoNombre: this.nombreQuejoso, fechaCita: this.fecha, horaCita: this.hora,
          tipoCita: this.modalidad, categoria: this.categoria, motivo: this.motivo.trim()
        });
    operacion.subscribe({
      next: (cita) => {
        this.guardando = false;
        this.snackBar.open(this.reagendando ? 'Cita reagendada. Se avisó al quejoso.' : 'Cita agendada. Se avisó al quejoso.',
          'Cerrar', { duration: 3500 });
        this.guardada.emit(cita);
      },
      error: (error) => {
        this.guardando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible guardar la cita.'), 'Cerrar', { duration: 4500 });
      }
    });
  }

  /** Entre semana y, si es hoy, todavía antes de la última hora de atención. */
  private esHabil(fecha: string): boolean {
    const d = new Date(fecha + 'T00:00:00');
    if (d.getDay() === 0 || d.getDay() === 6 || fecha < this.hoyIso) return false;
    return fecha > this.hoyIso || new Date().getHours() < 16;
  }

  private siguienteDiaHabil(): string {
    const d = new Date();
    if (d.getHours() >= 16) d.setDate(d.getDate() + 1);
    while (d.getDay() === 0 || d.getDay() === 6) d.setDate(d.getDate() + 1);
    return iso(d);
  }
}

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
