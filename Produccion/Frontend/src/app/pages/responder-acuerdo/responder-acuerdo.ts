import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AcuerdoService } from '../../core/services/acuerdo.service';
import { ToastService } from '../../core/services/toast.service';
import { AcuerdoPublico } from '../../core/models/acuerdo.models';

/** Máximo que acepta subdefensoria-service para el comentario. */
const COMENTARIO_LONGITUD_MAXIMA = 1000;

/**
 * CU-QJ-14: el quejoso acepta o rechaza (con motivo) el acuerdo de conciliación que le
 * propone Subdefensoría. Se llega desde el enlace del correo o al tocar la notificación del
 * panel: /acuerdo/responder?token=...  — no pide sesión, el token identifica el acuerdo.
 */
@Component({
  selector: 'app-responder-acuerdo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './responder-acuerdo.html',
  styleUrl: './responder-acuerdo.scss',
})
export class ResponderAcuerdo implements OnInit {
  readonly comentarioMaximo = COMENTARIO_LONGITUD_MAXIMA;

  token = '';
  acuerdo: AcuerdoPublico | null = null;
  cargando = true;
  errorCarga = '';

  /** 'aceptar' | 'rechazar' mientras el quejoso escribe; null mientras elige. */
  decision: 'aceptar' | 'rechazar' | null = null;
  comentario = '';
  intentoEnviar = false;
  enviando = false;

  constructor(
    private route: ActivatedRoute,
    private acuerdoService: AcuerdoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.cargando = false;
      this.errorCarga = 'El enlace del acuerdo está incompleto. Ábrelo de nuevo desde tu correo.';
      return;
    }

    this.acuerdoService.consultar(this.token).subscribe({
      next: (acuerdo) => {
        this.acuerdo = acuerdo;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.errorCarga = this.mensajeDeError(err, 'No se pudo cargar el acuerdo.');
        this.cdr.detectChanges();
      },
    });
  }

  get errorComentario(): string | null {
    if (this.decision === 'rechazar' && !this.comentario.trim()) {
      return 'Escribe el motivo por el que rechazas el acuerdo.';
    }
    if (this.comentario.length > COMENTARIO_LONGITUD_MAXIMA) {
      return `El comentario no puede exceder ${COMENTARIO_LONGITUD_MAXIMA} caracteres.`;
    }
    return null;
  }

  elegir(decision: 'aceptar' | 'rechazar'): void {
    this.decision = decision;
    this.intentoEnviar = false;
  }

  enviar(): void {
    this.intentoEnviar = true;
    if (!this.decision || this.errorComentario) return;

    this.enviando = true;
    const peticion = this.decision === 'aceptar'
      ? this.acuerdoService.aceptar(this.token, this.comentario.trim())
      : this.acuerdoService.rechazar(this.token, this.comentario.trim());
    const mensaje = this.decision === 'aceptar'
      ? 'Aceptaste el acuerdo. La Defensoría elaborará el acuerdo de conclusión.'
      : 'Rechazaste el acuerdo. La Defensoría continuará la investigación.';

    peticion.subscribe({
      next: (acuerdo) => {
        this.acuerdo = acuerdo;
        this.enviando = false;
        this.decision = null;
        this.toast.exito(mensaje);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.enviando = false;
        this.toast.error(this.mensajeDeError(err, 'No se pudo registrar tu respuesta.'));
        // El acuerdo pudo cambiar mientras tanto (ya respondido, sustituido): se vuelve a leer.
        this.acuerdoService.consultar(this.token).subscribe({
          next: (acuerdo) => {
            this.acuerdo = acuerdo;
            this.cdr.detectChanges();
          },
          error: () => this.cdr.detectChanges(),
        });
      },
    });
  }

  /** "13 de octubre de 2026" */
  fecha(fechaIso?: string | null): string {
    if (!fechaIso) return '';
    return new Date(fechaIso).toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  /** subdefensoria-service responde { "error": "..." }. */
  private mensajeDeError(err: unknown, porDefecto: string): string {
    const cuerpo = (err as { error?: { error?: string; mensaje?: string } })?.error;
    return cuerpo?.error ?? cuerpo?.mensaje ?? porDefecto;
  }
}
