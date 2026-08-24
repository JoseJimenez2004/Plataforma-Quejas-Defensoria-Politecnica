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
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { DictamenService } from '../../core/services/dictamen.service';

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

  // TODO: sustituir por los datos del analista autenticado cuando exista el módulo de sesión/login.
  private readonly analistaId = 1;
  private readonly analistaNombre = 'Analista Jurídico 01';

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

    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
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
  // IMPROCEDENCIA
  // =========================================================

  if (this.tipoDictamen === 'improcedente') {

    const confirmar = confirm(
      '¿Está seguro de declarar improcedente este expediente?'
    );

    if (!confirmar) return;

    this.dictamenService.registrarImprocedencia({
      folio: this.expediente.folio,
      analistaId: this.analistaId,
      analistaNombre: this.analistaNombre,
      justificacion: this.justificacion.trim()
    }).subscribe({
      next: () => {
        this.snackBar.open(
          'El expediente fue declarado improcedente correctamente.',
          'Cerrar',
          {
            duration: 3000
          }
        );

        this.router.navigate(['/bandeja']);
      },

      error: () => {
        this.snackBar.open(
          'No fue posible registrar la improcedencia.',
          'Cerrar',
          {
            duration: 3000
          }
        );
      }
    });

    return;
  }

  // =========================================================
  // COMPETENCIA → SUBDEFENSORÍA
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

  this.dictamenService.registrarCompetencia({
    folio: this.expediente.folio,
    analistaId: this.analistaId,
    analistaNombre: this.analistaNombre,
    justificacion: this.justificacion.trim(),
    areaTurno: 'Subdefensoría',
    responsableTurno: this.responsableTurno.trim(),
    observaciones: this.observaciones.trim() || undefined
  }).subscribe({
    next: () => {
      this.snackBar.open(
        'Expediente turnado a Subdefensoría correctamente.',
        'Cerrar',
        {
          duration: 3000
        }
      );

      this.router.navigate(['/bandeja']);
    },

    error: () => {
      this.snackBar.open(
        'No fue posible registrar el dictamen.',
        'Cerrar',
        {
          duration: 3000
        }
      );
    }
  });
}
}
