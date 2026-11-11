import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { RemisionService } from '../../core/services/remision.service';
import { InstanciaRemision, Remision as RemisionModelo } from '../../core/models/remision.model';
import { formatearFechaHora } from '../../core/utils/estatus-expediente';
import { descargarBlob, mensajeDeError } from '../../core/utils/archivos';
import { ConfirmacionService } from '../../core/services/confirmacion.service';

/**
 * CU-PC-09: remisión externa de un expediente improcedente.
 *
 * Un solo paso (2026-11-10): se elige la instancia del catálogo (OAG, OIC, COSECOVI /
 * Género; el correo lo administra la Defensora), se captura el fundamento y "Generar y
 * enviar oficio" genera el PDF y lo manda por correo. Solo si el correo sale queda ENVIADA,
 * el expediente REMITIDA y se avisa al quejoso. Después se puede ver o descargar el oficio.
 * Las remisiones que quedaron GENERADAS con el flujo anterior se envían eligiendo instancia.
 */
@Component({
  selector: 'app-remision',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatCheckboxModule,
    MatSnackBarModule
  ],
  templateUrl: './remision.html',
  styleUrl: './remision.css'
})
export class Remision implements OnInit {
  folio = '';
  /** Clave de la instancia elegida (OAG, OIC, COSECOVI). */
  institucion = '';
  fundamento = '';
  orientacion = '';
  adjuntarExpediente = true;

  expediente?: ExpedienteDetalle;
  remision?: RemisionModelo;

  cargando = true;
  procesando = false;

  readonly formatearFechaHora = formatearFechaHora;

  instituciones: InstanciaRemision[] = [];
  errorInstancias = false;

  private readonly confirmacion = inject(ConfirmacionService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private expedienteService: ExpedienteService,
    private remisionService: RemisionService,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('id') ?? '';
  }

  ngOnInit(): void {
    if (!this.folio) return;

    this.remisionService.obtenerInstancias().subscribe({
      next: (lista) => {
        this.instituciones = lista;
        this.errorInstancias = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorInstancias = true;
        this.cdr.detectChanges();
      }
    });

    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cargarRemision();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar los datos del expediente.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  private cargarRemision(): void {
    this.remisionService.obtenerPorFolio(this.folio).subscribe({
      next: (remision) => {
        this.remision = remision;
        // Para reintentar, se preselecciona la instancia que ya tenía.
        if (remision.estatus !== 'ENVIADA' && remision.instanciaClave) {
          this.institucion = remision.instanciaClave;
        }
        this.cargando = false;
        this.cdr.detectChanges();
      },
      // 404: todavía no hay remisión, se muestra el formulario.
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  get esImprocedente(): boolean {
    return this.expediente?.estatusCodigo === 'IMPROCEDENTE';
  }

  /** Paso activo del flujo: 0 datos de la remisión, 1 enviado. */
  get paso(): number {
    return this.remision?.estatus === 'ENVIADA' ? 1 : 0;
  }

  /** Instancia elegida en el formulario (para mostrar a qué correo saldrá). */
  get instanciaElegida(): InstanciaRemision | undefined {
    return this.instituciones.find(i => i.clave === this.institucion);
  }

  async generarYEnviar(): Promise<void> {
    if (!this.expediente?.folio) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', { duration: 3000 });
      return;
    }

    const instancia = this.instanciaElegida;
    if (!instancia || !this.fundamento.trim()) {
      this.snackBar.open(
        'Elige la instancia destino y escribe el fundamento de la remisión.',
        'Cerrar',
        { duration: 3500 }
      );
      return;
    }
    if (!instancia.correo) {
      this.snackBar.open(`${instancia.sigla} no tiene correo registrado. Pide a la Defensora que lo agregue.`, 'Cerrar', { duration: 4500 });
      return;
    }

    if (!(await this.confirmarEnvio(instancia))) return;

    this.procesando = true;
    this.cdr.detectChanges();

    this.remisionService.crearRemision({
      folio: this.expediente.folio,
      instanciaClave: instancia.clave,
      justificacionLegal: this.fundamento.trim(),
      sugerenciaQuejoso: this.orientacion.trim() || undefined,
      adjuntarExpediente: this.adjuntarExpediente
    }).subscribe({
      next: (remision) => this.alEnviar(remision),
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible enviar el oficio.'), 'Cerrar', {
          duration: 6000
        });
        // Si el correo falló, la remisión ya quedó guardada como pendiente: se muestra con
        // el botón «Enviar oficio» para reintentar (404 = no se guardó, sigue el formulario).
        this.cargarRemision();
      }
    });
  }

  /** Remisiones GENERADAS (flujo anterior o correo que falló) con el flujo anterior: se envían eligiendo instancia. */
  async enviarPendiente(): Promise<void> {
    const instancia = this.instanciaElegida;
    if (!instancia?.correo) {
      this.snackBar.open('Elige la instancia a la que se enviará el oficio.', 'Cerrar', { duration: 3500 });
      return;
    }

    if (!(await this.confirmarEnvio(instancia))) return;

    this.procesando = true;
    this.cdr.detectChanges();

    this.remisionService.enviarRemision(this.folio, instancia.clave).subscribe({
      next: (remision) => this.alEnviar(remision),
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible enviar el oficio.'), 'Cerrar', {
          duration: 5000
        });
      }
    });
  }

  private confirmarEnvio(instancia: InstanciaRemision): Promise<boolean> {
    return this.confirmacion.confirmar({
      titulo: '¿Generar y enviar el oficio?',
      mensaje: `Se enviará a ${instancia.sigla} (${instancia.correo}).`,
      nota: 'El expediente quedará como Remitido y se avisará al quejoso.',
      textoConfirmar: 'Sí, enviar oficio',
      icono: 'outgoing_mail'
    });
  }

  private alEnviar(remision: RemisionModelo): void {
    this.remision = remision;
    this.procesando = false;
    if (this.expediente) {
      this.expediente.estatus = 'Remitida';
      this.expediente.estatusCodigo = 'REMITIDA';
    }
    this.cdr.detectChanges();
    this.snackBar.open(
      `Oficio ${remision.numeroOficio} enviado a ${remision.correoDestino}. El expediente quedó remitido.`,
      'Cerrar',
      { duration: 5000 }
    );
  }

  /** Abre el oficio en una pestaña nueva (el navegador muestra el PDF). */
  verOficio(): void {
    const pestana = window.open('', '_blank');
    this.remisionService.descargarPdf(this.folio).subscribe({
      next: (pdf) => {
        const url = URL.createObjectURL(new Blob([pdf], { type: 'application/pdf' }));
        if (pestana) {
          pestana.location.href = url;
        } else {
          window.open(url, '_blank');
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: () => {
        pestana?.close();
        this.snackBar.open('No fue posible abrir el oficio.', 'Cerrar', { duration: 3500 });
      }
    });
  }

  descargarOficio(): void {
    this.remisionService.descargarPdf(this.folio).subscribe({
      next: (pdf) => descargarBlob(pdf, `oficio-remision-${this.expediente?.folioOrigen ?? this.folio}.pdf`),
      error: () => {
        this.snackBar.open('No fue posible descargar el oficio.', 'Cerrar', { duration: 3500 });
      }
    });
  }

  volver(): void {
    this.router.navigate(['/expediente', this.folio]);
  }
}
