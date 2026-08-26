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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { switchMap } from 'rxjs';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { RemisionService } from '../../core/services/remision.service';

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
  motivo = '';
  observaciones = '';

  expediente?: ExpedienteDetalle;

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

  enviarRemision(): void {
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

    const institucionDestino =
      this.institucion === 'Otra'
        ? this.institucionOtra.trim()
        : this.institucion;

    if (!institucionDestino || !this.fundamento.trim()) {
      this.snackBar.open(
        'Completa la institución destino y el fundamento de la remisión.',
        'Cerrar',
        {
          duration: 3500
        }
      );
      return;
    }

    const confirmar = confirm(
      'La remisión cerrará el análisis de Primer Contacto para este expediente. ¿Desea continuar?'
    );

    if (!confirmar) return;

    const folio = this.expediente.folio;

    this.remisionService.crearRemision({
      folio,
      autoridadRemision: institucionDestino,
      justificacionLegal: this.fundamento.trim(),
      sugerenciaQuejoso:
        this.motivo.trim() ||
        this.observaciones.trim() ||
        undefined,
      adjuntarExpediente: true
    }).pipe(
      switchMap(() =>
        this.remisionService.enviarRemision(folio)
      )
    ).subscribe({
      next: () => {
        this.snackBar.open(
          'Expediente remitido correctamente.',
          'Cerrar',
          {
            duration: 3000
          }
        );

        this.router.navigate(['/bandeja']);
      },

      error: () => {
        this.snackBar.open(
          'No fue posible registrar la remisión.',
          'Cerrar',
          {
            duration: 3000
          }
        );
      }
    });
  }
}
