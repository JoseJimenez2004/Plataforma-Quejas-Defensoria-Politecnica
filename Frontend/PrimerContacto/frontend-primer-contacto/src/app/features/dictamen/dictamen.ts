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

  guardarBorrador(): void {
    this.snackBar.open('Dictamen preliminar guardado.', 'Cerrar', {
      duration: 3000
    });
  }

  enviarTitular(): void {
    if (!this.expediente?.quejaId) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    if (!this.justificacion.trim()) {
      this.snackBar.open('Ingresa la justificación del dictamen.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    const confirmar = confirm('¿Está seguro de enviar este expediente al Titular?');

    if (!confirmar) return;

    this.dictamenService.registrarCompetencia({
      quejaId: this.expediente.quejaId,
      folio: this.folio,
      analistaId: this.analistaId,
      analistaNombre: this.analistaNombre,
      justificacion: this.justificacion,
      areaTurno: 'Titular de la Defensoría',
      responsableTurno: this.observaciones.trim() || 'Sin observaciones adicionales'
    }).subscribe({
      next: () => {
        this.snackBar.open('Expediente enviado al Titular correctamente.', 'Cerrar', {
          duration: 3000
        });
        this.router.navigate(['/bandeja']);
      },
      error: () => {
        this.snackBar.open('No fue posible registrar el dictamen.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }
}
