import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
import { Remision as RemisionModelo } from '../../core/models/remision.model';
import { formatearFechaHora } from '../../core/utils/estatus-expediente';
import { descargarBlob, mensajeDeError } from '../../core/utils/archivos';

/**
 * CU-PC-09: remisión externa de un expediente improcedente.
 *
 *   1. Se capturan los datos -> la remisión queda GENERADA y ya se puede descargar el
 *      oficio en PDF.
 *   2. El analista entrega el oficio por los medios oficiales y registra el envío ->
 *      remisión ENVIADA y expediente REMITIDA. Se avisa al quejoso.
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
  institucion = '';
  institucionOtra = '';
  fundamento = '';
  orientacion = '';
  adjuntarExpediente = true;

  expediente?: ExpedienteDetalle;
  remision?: RemisionModelo;

  cargando = true;
  procesando = false;

  readonly formatearFechaHora = formatearFechaHora;

  instituciones = [
    'Órgano Interno de Control IPN',
    'Abogado General',
    'Comisión de Derechos Humanos',
    'Ministerio Público',
    'Otra'
  ];

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

  /** Paso activo del flujo: 0 datos, 1 oficio generado, 2 enviado. */
  get paso(): number {
    if (!this.remision) return 0;
    return this.remision.estatus === 'ENVIADA' ? 2 : 1;
  }

  generarRemision(): void {
    if (!this.expediente?.folio) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', { duration: 3000 });
      return;
    }

    const institucionDestino =
      this.institucion === 'Otra'
        ? this.institucionOtra.trim()
        : this.institucion;

    if (!institucionDestino || !this.fundamento.trim()) {
      this.snackBar.open(
        'Completa la institución destino y el fundamento de la remisión.',
        'Cerrar',
        { duration: 3500 }
      );
      return;
    }

    this.procesando = true;

    this.remisionService.crearRemision({
      folio: this.expediente.folio,
      autoridadRemision: institucionDestino,
      justificacionLegal: this.fundamento.trim(),
      sugerenciaQuejoso: this.orientacion.trim() || undefined,
      adjuntarExpediente: this.adjuntarExpediente
    }).subscribe({
      next: (remision) => {
        this.remision = remision;
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(
          `Oficio ${remision.numeroOficio} generado. Descárgalo y registra su envío.`,
          'Cerrar',
          { duration: 4000 }
        );
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible generar la remisión.'), 'Cerrar', {
          duration: 4000
        });
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

  registrarEnvio(): void {
    const confirmar = confirm(
      'Registra el envío solo cuando el oficio ya se haya entregado a la instancia. '
        + 'El expediente quedará como Remitido y se avisará al quejoso. ¿Continuar?'
    );

    if (!confirmar) return;

    this.procesando = true;

    this.remisionService.enviarRemision(this.folio).subscribe({
      next: (remision) => {
        this.remision = remision;
        this.procesando = false;
        if (this.expediente) {
          this.expediente.estatus = 'Remitida';
          this.expediente.estatusCodigo = 'REMITIDA';
        }
        this.cdr.detectChanges();
        this.snackBar.open('Envío registrado. El expediente quedó remitido.', 'Cerrar', { duration: 3500 });
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible registrar el envío.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }

  volver(): void {
    this.router.navigate(['/expediente', this.folio]);
  }
}
