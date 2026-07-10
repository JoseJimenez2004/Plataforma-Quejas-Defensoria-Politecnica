import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { Subject, of } from 'rxjs';
import { catchError, switchMap, map } from 'rxjs/operators';

import { BandejaService } from '../../core/services/bandeja.service';
import { ExpedienteBandeja } from '../../core/models/expediente-bandeja';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedientePrimerContacto } from '../../core/models/expediente-primer-contacto';
import { AgendaService } from '../../core/services/agenda.service';
import { CitaDetalleDialog } from '../../shared/cita-detalle-dialog/cita-detalle-dialog';

import {
  CitaPrimerContacto,
  CrearCitaPrimerContacto
} from '../../core/models/cita-primer-contacto';

@Component({
  selector: 'app-agenda',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './agenda.html',
  styleUrl: './agenda.css'
})
export class Agenda implements OnInit {
  diasSemana = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

  meses = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];

  hoy = new Date();

  anioSeleccionado = this.hoy.getFullYear();
  mesSeleccionado = this.hoy.getMonth() + 1;
  diaSeleccionado = this.hoy.getDate();

  diasMes: number[] = [];
  espaciosIniciales: number[] = [];
  fechaAgendaSeleccionada = '';

  expedienteActual?: ExpedientePrimerContacto;
  expedientesPendientesCita: ExpedienteBandeja[] = [];

  // Si tiene valor, agendarCita() está reagendando esta cita (se cancela la
  // original antes de crear la nueva) en vez de dar de alta una cita normal.
  citaIdEnReagenda: number | null = null;

  citaNueva: CitaPrimerContacto = {
    folio: '',
    quejoso: '',
    fecha: '',
    hora: '',
    tipo: 'Presencial',
    motivo: '',
    estatus: 'Programada'
  };

  citasDelDia: CitaPrimerContacto[] = [];

  private fechaAgendaSubject = new Subject<string>();

  horariosSugeridos = [
    '09:00', '09:30',
    '10:00', '10:30',
    '11:00', '11:30',
    '12:00', '12:30',
    '13:00', '13:30',
    '15:00', '15:30',
    '16:00', '16:30',
    '17:00'
  ];

  constructor(
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private expedienteService: ExpedienteService,
    private agendaService: AgendaService,
    private bandejaService: BandejaService,
    private cdr: ChangeDetectorRef,
    private zone: NgZone
  ) {
    const folio = history.state?.folio;

    if (folio) {
      this.citaNueva.folio = folio;
      this.cargarExpediente(folio);
    }
  }

ngOnInit(): void {
  this.fechaAgendaSubject.pipe(
    switchMap((fechaVista) => {
      const fechaBackend = this.convertirFechaBackend(fechaVista);

      return this.agendaService.obtenerAgendaDia(fechaBackend).pipe(
        map((citas) => ({
          fechaVista,
          citas
        })),
        catchError(() => {
          this.snackBar.open('No fue posible cargar la agenda del día.', 'Cerrar', {
            duration: 3000
          });

          return of({
            fechaVista,
            citas: []
          });
        })
      );
    })
  ).subscribe(({ fechaVista, citas }) => {
  if (fechaVista !== this.fechaAgendaSeleccionada) return;

  this.zone.run(() => {
    this.citasDelDia = [...citas];
    this.cdr.detectChanges();
  });
});

  this.generarCalendario();
  this.seleccionarDia(this.diaSeleccionado);
  this.cargarExpedientesPendientes();
}

  get nombreMesSeleccionado(): string {
    return this.meses[this.mesSeleccionado - 1];
  }

  generarCalendario(): void {
    const totalDias = new Date(
      this.anioSeleccionado,
      this.mesSeleccionado,
      0
    ).getDate();

    const primerDiaSemana = new Date(
      this.anioSeleccionado,
      this.mesSeleccionado - 1,
      1
    ).getDay();

    this.diasMes = Array.from({ length: totalDias }, (_, i) => i + 1);
    this.espaciosIniciales = Array.from({ length: primerDiaSemana }, (_, i) => i);
  }

  mesAnterior(): void {
    if (this.mesSeleccionado === 1) {
      this.mesSeleccionado = 12;
      this.anioSeleccionado--;
    } else {
      this.mesSeleccionado--;
    }

    this.diaSeleccionado = 1;
    this.generarCalendario();
    this.seleccionarDia(1);
  }

  mesSiguiente(): void {
    if (this.mesSeleccionado === 12) {
      this.mesSeleccionado = 1;
      this.anioSeleccionado++;
    } else {
      this.mesSeleccionado++;
    }

    this.diaSeleccionado = 1;
    this.generarCalendario();
    this.seleccionarDia(1);
  }

  irAHoy(): void {
    this.hoy = new Date();

    this.anioSeleccionado = this.hoy.getFullYear();
    this.mesSeleccionado = this.hoy.getMonth() + 1;
    this.diaSeleccionado = this.hoy.getDate();

    this.generarCalendario();
    this.seleccionarDia(this.diaSeleccionado);
  }

  seleccionarDia(dia: number): void {
    this.diaSeleccionado = dia;

    const diaFormateado = dia.toString().padStart(2, '0');
    const mesFormateado = this.mesSeleccionado.toString().padStart(2, '0');

    this.fechaAgendaSeleccionada =
      `${diaFormateado}/${mesFormateado}/${this.anioSeleccionado}`;

    this.citaNueva.fecha = this.fechaAgendaSeleccionada;

    this.cargarAgendaPorFecha(this.fechaAgendaSeleccionada);
  }

cargarAgendaPorFecha(fechaVista: string): void {
  if (!fechaVista) return;

  this.zone.run(() => {
    this.citasDelDia = [];
    this.cdr.detectChanges();
  });

  this.fechaAgendaSubject.next(fechaVista);
}
  cargarExpedientesPendientes(): void {
    this.bandejaService.obtenerBandeja().subscribe({
      next: (expedientes) => {
        this.expedientesPendientesCita = expedientes.filter(
          expediente => expediente.estatus === 'Pendiente'
        );
      },
      error: () => {
        this.snackBar.open('No fue posible cargar los expedientes pendientes.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  seleccionarExpedientePendiente(folio: string): void {
    this.cargarExpediente(folio);
  }

  cargarExpediente(folio: string): void {
    this.expedienteService.obtenerPorFolio(folio).subscribe({
      next: (expediente) => {
        this.expedienteActual = expediente;
        this.citaNueva.folio = expediente.folio;
        this.citaNueva.quejoso = expediente.quejoso.nombreCompleto;
        this.citaNueva.motivo = expediente.descripcionHechos;
      },
      error: () => {
        this.snackBar.open('No fue posible cargar los datos del expediente.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  contarCitasPorDia(dia: number): number {
    if (dia !== this.diaSeleccionado) return 0;
    return this.citasDelDia.length;
  }

  obtenerCitasDelDiaSeleccionado(): CitaPrimerContacto[] {
    return this.citasDelDia;
  }

  agendarCita(): void {
    this.formatearHora();

    if (!this.citaNueva.folio || !this.citaNueva.fecha || !this.citaNueva.hora || !this.citaNueva.motivo) {
      this.snackBar.open('Completa los campos obligatorios.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    if (!this.horaValida()) {
      this.snackBar.open('La hora debe estar en formato 24 horas. Ejemplo: 09:30 o 14:15.', 'Cerrar', {
        duration: 3500
      });
      return;
    }

    if (this.horaOcupada()) {
      this.snackBar.open('Ya existe una cita programada para esa fecha y hora.', 'Cerrar', {
        duration: 3500
      });
      return;
    }

    if (!this.expedienteActual) {
      this.snackBar.open('No hay expediente cargado para agendar la cita.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    const fechaSeleccionada = this.fechaAgendaSeleccionada;

    const dto: CrearCitaPrimerContacto = {
      quejaId: this.expedienteActual.quejaId,
      folio: this.expedienteActual.folio,
      quejosoId: this.expedienteActual.quejoso.id,
      quejosoNombre: this.expedienteActual.quejoso.nombreCompleto,
      analistaId: 1,
      analistaNombre: 'Analista Jurídico 01',
      fechaCita: this.convertirFechaBackend(fechaSeleccionada),
      horaCita: this.citaNueva.hora,
      tipoCita: this.citaNueva.tipo === 'Virtual' ? 'VIRTUAL' : 'PRESENCIAL',
      motivo: this.citaNueva.motivo
    };

    const idACancelar = this.citaIdEnReagenda;

    const registrarCitaNueva = () => {
      this.agendaService.crearCita(dto).subscribe({
        next: () => {
          this.expedienteActual = undefined;
          this.citaIdEnReagenda = null;

          this.citaNueva = {
            folio: '',
            quejoso: '',
            fecha: fechaSeleccionada,
            hora: '',
            tipo: 'Presencial',
            motivo: '',
            estatus: 'Programada'
          };

          this.cargarAgendaPorFecha(fechaSeleccionada);
          this.cargarExpedientesPendientes();

          const mensaje = idACancelar ? 'Cita reagendada correctamente.' : 'Cita agendada correctamente.';
          this.snackBar.open(mensaje, 'Cerrar', { duration: 3000 });
        },
        error: () => {
          const mensaje = idACancelar
            ? 'Se canceló la cita original, pero no fue posible crear la nueva. Vuelve a agendar desde el expediente.'
            : 'No fue posible registrar la cita.';
          this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
        }
      });
    };

    // El backend rechaza crear una cita si el folio ya tiene una activa
    // (existsByFolioAndEstatusNot), así que si estamos reagendando hay que
    // cancelar la cita original ANTES de crear la nueva, no después.
    if (idACancelar) {
      this.agendaService.cancelarCita(idACancelar).subscribe({
        next: () => registrarCitaNueva(),
        error: () => {
          this.snackBar.open('No fue posible cancelar la cita original para reagendarla.', 'Cerrar', {
            duration: 3500
          });
        }
      });
    } else {
      registrarCitaNueva();
    }
  }

  abrirDetalle(cita: CitaPrimerContacto): void {
    const dialogRef = this.dialog.open(CitaDetalleDialog, {
      width: '720px',
      data: cita
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (!resultado) return;

      if (resultado.accion === 'reagendar') {
        // Bug 1: antes no se recargaba el expediente, así que expedienteActual
        // quedaba vacío y agendarCita() fallaba con "No hay expediente cargado".
        this.cargarExpediente(resultado.cita.folio);

        // Bug 2: no bastaba con precargar el formulario; sin esto,
        // agendarCita() crea una cita nueva y deja la original activa (duplicado).
        this.citaIdEnReagenda = resultado.cita.id ?? null;

        this.citaNueva = {
          ...resultado.cita,
          fecha: this.fechaAgendaSeleccionada,
          hora: ''
        };

        this.snackBar.open('Elige nueva fecha/hora y confirma para reagendar.', 'Cerrar', {
          duration: 3500
        });
      }

      if (resultado.accion === 'cancelar') {
        if (!resultado.cita.id) return;

        this.agendaService.cancelarCita(resultado.cita.id).subscribe({
          next: () => {
            this.cargarAgendaPorFecha(this.fechaAgendaSeleccionada);

            this.snackBar.open('Cita cancelada correctamente.', 'Cerrar', {
              duration: 3000
            });
          },
          error: () => {
            this.snackBar.open('No fue posible cancelar la cita.', 'Cerrar', {
              duration: 3000
            });
          }
        });
      }
    });
  }

  convertirFechaBackend(fecha: string): string {
    const [day, month, year] = fecha.split('/');
    return `${year}-${month}-${day}`;
  }

  formatearHora(): void {
    let hora = this.citaNueva.hora.trim();
    hora = hora.replace('.', ':');

    const partes = hora.split(':');

    if (partes.length === 2) {
      const horas = partes[0].padStart(2, '0');
      const minutos = partes[1].padStart(2, '0');
      this.citaNueva.hora = `${horas}:${minutos}`;
    }
  }

  horaValida(): boolean {
    return /^([01]\d|2[0-3]):[0-5]\d$/.test(this.citaNueva.hora);
  }

  horaOcupada(): boolean {
    return this.citasDelDia.some(cita =>
      cita.fecha === this.fechaAgendaSeleccionada &&
      cita.hora === this.citaNueva.hora &&
      cita.estatus !== 'Cancelada'
    );
  }

  obtenerHorariosDisponibles(): string[] {
    return this.horariosSugeridos.filter(hora =>
      !this.citasDelDia.some(cita =>
        cita.fecha === this.fechaAgendaSeleccionada &&
        cita.hora === hora &&
        cita.estatus !== 'Cancelada'
      )
    );
  }

  seleccionarHora(hora: string): void {
    this.citaNueva.hora = hora;
  }
}