import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedienteDetalle, EvidenciaDetalle } from '../../core/models/expediente-detalle';
import { DictamenService } from '../../core/services/dictamen.service';
import { estaAbierto } from '../../core/utils/estatus-expediente';
import { abrirBlob, mensajeDeError } from '../../core/utils/archivos';

@Component({
  selector: 'app-dictamen',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatSnackBarModule
  ],
  templateUrl: './dictamen.html',
  styleUrl: './dictamen.css'
})
export class Dictamen implements OnInit {
  folio = '';
  justificacion = '';
  observaciones = '';
  responsableTurno = '';
  tipoDictamen: 'competente' | 'improcedente' = 'competente';

  expediente?: ExpedienteDetalle;

  /** El expediente ya no admite dictamen (cerrado o ya dictaminado). */
  bloqueado = false;
  enviando = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private expedienteService: ExpedienteService,
    private dictamenService: DictamenService,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('id') ?? '';

    this.tipoDictamen =
      this.route.snapshot.queryParamMap.get('tipo') === 'improcedente'
        ? 'improcedente'
        : 'competente';
  }

  ngOnInit(): void {
    if (!this.folio) return;

    /*
     * CU-PC-08: antes, si el expediente ya tenía dictamen, el sistema no avisaba y solo
     * fallaba al guardar. Ahora se detecta al entrar y se lleva a la consulta.
     */
    this.dictamenService.obtenerPorFolio(this.folio).subscribe({
      next: () => {
        this.bloqueado = true;
        this.snackBar.open(
          'Este expediente ya tiene un dictamen registrado. Te mostramos la consulta.',
          'Cerrar',
          { duration: 4000 }
        );
        this.router.navigate(['/dictamen', this.folio, 'consulta']);
      },
      error: () => this.cargarExpediente()
    });
  }

  private cargarExpediente(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.bloqueado = !estaAbierto(this.expediente.estatusCodigo);
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar los datos del expediente.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  abrirEvidencia(evidencia: EvidenciaDetalle): void {
    this.expedienteService.descargarEvidencia(evidencia.id).subscribe({
      next: (archivo) => abrirBlob(archivo),
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible abrir la evidencia.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  enviarTitular(): void {
    if (!this.expediente?.folio) {
      this.snackBar.open(
        'No fue posible identificar el expediente.',
        'Cerrar',
        {
          duration: 3000
        }
      );
      return;
    }

    if (!this.justificacion.trim()) {
      this.snackBar.open(
        'Ingresa la justificación del dictamen.',
        'Cerrar',
        {
          duration: 3000
        }
      );
      return;
    }

    // =========================================================
    // IMPROCEDENCIA -> sigue la remisión externa (CU-PC-09)
    // =========================================================

    if (this.tipoDictamen === 'improcedente') {

      const confirmar = confirm(
        '¿Está seguro de declarar improcedente este expediente? Después deberá generar la remisión a la instancia competente.'
      );

      if (!confirmar) return;

      this.enviando = true;

      this.dictamenService.registrarImprocedencia({
        folio: this.expediente.folio,
        justificacion: this.justificacion.trim()
      }).subscribe({
        next: () => {
          this.snackBar.open(
            'Expediente declarado improcedente. Ahora genera la remisión.',
            'Cerrar',
            {
              duration: 3500
            }
          );

          this.router.navigate(['/remision', this.folio]);
        },

        error: (error) => {
          this.enviando = false;
          this.cdr.detectChanges();
          this.snackBar.open(
            mensajeDeError(error, 'No fue posible registrar la improcedencia.'),
            'Cerrar',
            {
              duration: 4000
            }
          );
        }
      });

      return;
    }

    // =========================================================
    // COMPETENCIA → PROCEDENTE → SUBDEFENSORÍA
    // =========================================================

    if (!this.responsableTurno.trim()) {
      this.snackBar.open(
        'Ingresa el responsable de Subdefensoría.',
        'Cerrar',
        {
          duration: 3000
        }
      );
      return;
    }

    const confirmar = confirm(
      '¿Está seguro de turnar este expediente a Subdefensoría?'
    );

    if (!confirmar) return;

    this.enviando = true;

    this.dictamenService.registrarCompetencia({
      folio: this.expediente.folio,
      justificacion: this.justificacion.trim(),
      areaTurno: 'Subdefensoría',
      responsableTurno: this.responsableTurno.trim(),
      observaciones: this.observaciones.trim() || undefined
    }).subscribe({
      next: (dictamen) => {
        const mensaje = dictamen.folioSubdefensoria
          ? `Expediente procedente y recibido por Subdefensoría (${dictamen.folioSubdefensoria}).`
          : 'El dictamen quedó registrado, pero Subdefensoría no respondió. Reintenta el envío desde el expediente.';

        this.snackBar.open(mensaje, 'Cerrar', { duration: 4500 });

        this.router.navigate(['/expediente', this.folio]);
      },

      error: (error) => {
        this.enviando = false;
        this.cdr.detectChanges();
        this.snackBar.open(
          mensajeDeError(error, 'No fue posible registrar el dictamen.'),
          'Cerrar',
          {
            duration: 4000
          }
        );
      }
    });
  }
}
