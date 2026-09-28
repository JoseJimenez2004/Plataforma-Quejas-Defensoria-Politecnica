import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Dictamen } from '../../core/models/dictamen.model';
import { DictamenService } from '../../core/services/dictamen.service';
import {
  claseEstatus,
  etiquetaEstatus,
  formatearFechaHora
} from '../../core/utils/estatus-expediente';
import { mensajeDeError } from '../../core/utils/archivos';

/**
 * CU-PC-08: consulta del dictamen ya registrado de un expediente. El backend ya tenía
 * GET /dictamenes/folio/{folio}; faltaba la pantalla.
 */
@Component({
  selector: 'app-dictamen-consulta',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  templateUrl: './dictamen-consulta.html',
  styleUrl: './dictamen-consulta.css'
})
export class DictamenConsulta implements OnInit {
  folio = '';
  dictamen?: Dictamen;

  cargando = true;
  sinDictamen = false;
  procesando = false;

  readonly claseEstatus = claseEstatus;
  readonly etiquetaEstatus = etiquetaEstatus;
  readonly formatearFechaHora = formatearFechaHora;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dictamenService: DictamenService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('id') ?? '';
  }

  ngOnInit(): void {
    this.dictamenService.obtenerPorFolio(this.folio).subscribe({
      next: (dictamen) => {
        this.dictamen = dictamen;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.cargando = false;
        this.sinDictamen = error.status === 404;
        this.cdr.detectChanges();

        if (!this.sinDictamen) {
          this.snackBar.open('No fue posible cargar el dictamen.', 'Cerrar', { duration: 3500 });
        }
      }
    });
  }

  get esCompetente(): boolean {
    return this.dictamen?.resultado === 'COMPETENTE';
  }

  get pendienteSubdefensoria(): boolean {
    return this.esCompetente && !this.dictamen?.folioSubdefensoria;
  }

  volver(): void {
    this.router.navigate(['/expediente', this.folio]);
  }

  irARemision(): void {
    this.router.navigate(['/remision', this.folio]);
  }

  reenviar(): void {
    this.procesando = true;

    this.dictamenService.reenviarASubdefensoria(this.folio).subscribe({
      next: (dictamen) => {
        this.dictamen = dictamen;
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open('Subdefensoría recibió el expediente.', 'Cerrar', { duration: 3000 });
      },
      error: (error) => {
        this.procesando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible reenviar a Subdefensoría.'), 'Cerrar', {
          duration: 4000
        });
      }
    });
  }
}
