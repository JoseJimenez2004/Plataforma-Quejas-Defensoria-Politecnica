import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CitaService } from '../../core/services/cita.service';
import { ToastService } from '../../core/services/toast.service';
import { CitaPublica } from '../../core/models/cita.models';

/** Máximo que acepta primer-contacto-service para el motivo de cancelación. */
const MOTIVO_LONGITUD_MAXIMA = 1000;

/**
 * El quejoso confirma o cancela (con motivo) la cita que le agendó Primer Contacto. Se llega
 * desde el enlace del correo o al tocar la notificación del panel:
 * /cita/responder?token=...  — no pide sesión, el token identifica la cita.
 */
@Component({
  selector: 'app-responder-cita',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './responder-cita.html',
  styleUrl: './responder-cita.scss',
})
export class ResponderCita implements OnInit {
  readonly motivoMaximo = MOTIVO_LONGITUD_MAXIMA;

  token = '';
  cita: CitaPublica | null = null;
  cargando = true;
  errorCarga = '';

  cancelando = false;
  motivo = '';
  intentoCancelar = false;
  enviando = false;

  constructor(
    private route: ActivatedRoute,
    private citaService: CitaService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.cargando = false;
      this.errorCarga = 'El enlace de la cita está incompleto. Ábrelo de nuevo desde tu correo.';
      return;
    }

    this.citaService.consultar(this.token).subscribe({
      next: (cita) => {
        this.cita = cita;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.errorCarga = this.mensajeDeError(err, 'No se pudo cargar tu cita.');
        this.cdr.detectChanges();
      },
    });
  }

  get errorMotivo(): string | null {
    if (!this.motivo.trim()) return 'Escribe el motivo por el que cancelas.';
    if (this.motivo.length > MOTIVO_LONGITUD_MAXIMA) {
      return `El motivo no puede exceder ${MOTIVO_LONGITUD_MAXIMA} caracteres.`;
    }
    return null;
  }

  confirmar(): void {
    this.enviando = true;
    this.citaService.confirmar(this.token).subscribe({
      next: (cita) => this.respuestaGuardada(cita, 'Confirmaste tu asistencia a la cita.'),
      error: (err) => this.errorAlResponder(err, 'No se pudo confirmar la cita.'),
    });
  }

  cancelar(): void {
    this.intentoCancelar = true;
    if (this.errorMotivo) return;

    this.enviando = true;
    this.citaService.cancelar(this.token, this.motivo.trim()).subscribe({
      next: (cita) => this.respuestaGuardada(cita, 'Cancelaste la cita. Primer Contacto te propondrá otra fecha.'),
      error: (err) => this.errorAlResponder(err, 'No se pudo cancelar la cita.'),
    });
  }

  /** "jueves 15 de octubre de 2026" */
  fechaLarga(fecha?: string): string {
    if (!fecha) return '';
    const [a, m, d] = fecha.split('-').map(Number);
    return new Date(a, m - 1, d).toLocaleDateString('es-MX', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  hora(hora?: string): string {
    return hora ? hora.substring(0, 5) : '';
  }

  /** "10 de octubre a las 09:12" */
  fechaHora(fechaIso?: string): string {
    if (!fechaIso) return '';
    const fecha = new Date(fechaIso);
    return (
      fecha.toLocaleDateString('es-MX', { day: 'numeric', month: 'long' }) +
      ' a las ' +
      fecha.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', hour12: false })
    );
  }

  modalidad(tipo?: string): string {
    return tipo === 'VIRTUAL' ? 'Virtual' : 'Presencial';
  }

  private respuestaGuardada(cita: CitaPublica, mensaje: string): void {
    this.cita = cita;
    this.enviando = false;
    this.cancelando = false;
    this.toast.exito(mensaje);
    this.cdr.detectChanges();
  }

  private errorAlResponder(err: unknown, porDefecto: string): void {
    this.enviando = false;
    this.toast.error(this.mensajeDeError(err, porDefecto));
    // La cita pudo cambiar mientras tanto (plazo vencido, ya respondida): se vuelve a leer.
    this.citaService.consultar(this.token).subscribe({
      next: (cita) => {
        this.cita = cita;
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges(),
    });
  }

  /** primer-contacto-service responde { "error": "..." }. */
  private mensajeDeError(err: unknown, porDefecto: string): string {
    const cuerpo = (err as { error?: { error?: string; mensaje?: string } })?.error;
    return cuerpo?.error ?? cuerpo?.mensaje ?? porDefecto;
  }
}
