import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { ExpedienteService } from '../../core/services/expediente.service';
import { NotaAnalisisService } from '../../core/services/nota-analisis.service';

@Component({
  selector: 'app-expediente',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule
  ],
  templateUrl: './expediente.html',
  styleUrl: './expediente.css'
})
export class Expediente implements OnInit {
  folio = '';

  nuevaNota = '';

  expediente: ExpedienteDetalle = {
    folio: '',
    asunto: '',
    fechaIngreso: '',
    estatus: '',
    prioridad: 'Media',
    narrativa: '',
    quejoso: {
      nombre: '',
      boleta: '',
      correo: '',
      telefono: '',
      unidadAcademica: ''
    },
    evidencias: [],
    notas: []
  };

  cargando = false;
  sinDatos = false;


constructor(
  private route: ActivatedRoute,
  private router: Router,
  private expedienteService: ExpedienteService,
  private notaAnalisisService: NotaAnalisisService,
  private snackBar: MatSnackBar,
  private cdr: ChangeDetectorRef
) {}

ngOnInit(): void {
  this.route.paramMap.subscribe(params => {
    this.folio = params.get('id') ?? '';

    if (this.folio) {
      this.cargarExpediente();
    }
  });
}

cargarExpediente(): void {
  this.cargando = true;
  this.sinDatos = false;

  this.expedienteService.obtenerPorFolio(this.folio).subscribe({
    next: (expediente) => {
      this.expediente = this.expedienteService.mapearADetalle(expediente);
      this.cargando = false;
      this.cdr.detectChanges();
    },
    error: (error) => {
      this.cargando = false;

      if (error.status === 404) {
        this.sinDatos = true;
        this.cdr.detectChanges();
        this.snackBar.open(
          `No se ha recibido de Revisión ningún expediente con folio ${this.folio}.`,
          'Cerrar',
          { duration: 4000 }
        );
        return;
      }

      this.cdr.detectChanges();
      this.snackBar.open(
        'No fue posible cargar el expediente. Intenta nuevamente o contacta al administrador del sistema.',
        'Cerrar',
        { duration: 4000 }
      );
    }
  });
}

  agregarNota(): void {
    const nota = this.nuevaNota.trim();

    if (!nota) return;

    if (!this.expediente.folio) {
      this.snackBar.open('No fue posible identificar el expediente.', 'Cerrar', {
        duration: 3000
      });
      return;
    }

    this.notaAnalisisService.crearNota({
      folio: this.expediente.folio,
      contenido: nota
    }).subscribe({
      next: (notaGuardada) => {
        this.expediente.notas.push(notaGuardada.contenido);
        this.nuevaNota = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible guardar la nota.', 'Cerrar', {
          duration: 3000
        });
      }
    });
  }

  marcarCompetente(): void {
    this.router.navigate(['/dictamen', this.expediente.folio]);
  }

  marcarImprocedente(): void {
    this.router.navigate(
      ['/dictamen', this.expediente.folio],
      {
        queryParams: {
          tipo: 'improcedente'
        }
      }
    );
  }

  agendarCita(): void {
    this.router.navigate(['/agenda'], {
      state: {
        folio: this.expediente.folio
      }
    });
  }
}