import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Antecedente, BusquedaAntecedentes } from '../../core/models/antecedentes.model';
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { AntecedentesService } from '../../core/services/antecedentes.service';
import { ExpedienteService } from '../../core/services/expediente.service';
import { NotaAnalisisService } from '../../core/services/nota-analisis.service';
import { formatearFecha } from '../../core/utils/estatus-expediente';
import { mensajeDeError } from '../../core/utils/archivos';

/** Estatus de quejas de cualquier etapa (recepción y primer contacto). */
const ETIQUETAS_QUEJA: Record<string, string> = {
  RECIBIDA: 'Recibida',
  EN_VALIDACION: 'En validación',
  RECHAZADA: 'Rechazada',
  CORREGIDA: 'Corregida',
  CANCELADA: 'Cancelada',
  TURNADA: 'Turnada',
  EN_ANALISIS: 'En análisis',
  PROCEDENTE: 'Procedente',
  IMPROCEDENTE: 'Improcedente',
  REMITIDA: 'Remitida'
};

/**
 * Búsqueda de antecedentes de la queja que se analiza: otras quejas del sistema que
 * podrían estar relacionadas (mismo quejoso, mismos hechos, misma escuela...).
 *
 * El "parecido" lo calcula el backend con el motor que tenga conectado. Hoy es un motor
 * provisional por reglas; cuando llegue el modelo, esta pantalla no cambia.
 */
@Component({
  selector: 'app-antecedentes',
  imports: [
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatButtonToggleModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './antecedentes.html',
  styleUrl: './antecedentes.css'
})
export class Antecedentes implements OnInit {
  folio = '';

  expediente?: ExpedienteDetalle;
  busqueda?: BusquedaAntecedentes;

  buscando = false;
  error = false;

  soloMismoQuejoso = false;
  similitudMinima = 0;

  /** Folios de quejas ya registradas como antecedente en las notas de esta sesión. */
  registrados = new Set<string>();

  readonly formatearFecha = formatearFecha;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private antecedentesService: AntecedentesService,
    private expedienteService: ExpedienteService,
    private notaService: NotaAnalisisService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('id') ?? '';
  }

  ngOnInit(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        // Las notas "Antecedente relacionado: FOL-..." ya registradas no se vuelven a ofrecer.
        this.expediente.notas
          .map(n => /Antecedente relacionado: (\S+)/.exec(n.contenido)?.[1])
          .filter((f): f is string => !!f)
          .forEach(f => this.registrados.add(f));
        this.cdr.detectChanges();
      }
    });

    this.buscar();
  }

  buscar(): void {
    this.buscando = true;
    this.error = false;
    this.cdr.detectChanges();

    this.antecedentesService.buscar(this.folio).subscribe({
      next: (busqueda) => {
        this.busqueda = busqueda;
        this.buscando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.buscando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  get resultados(): Antecedente[] {
    return (this.busqueda?.resultados ?? []).filter(a =>
      (!this.soloMismoQuejoso || a.mismoQuejoso) && a.similitud >= this.similitudMinima
    );
  }

  get totalMismoQuejoso(): number {
    return (this.busqueda?.resultados ?? []).filter(a => a.mismoQuejoso).length;
  }

  get esProvisional(): boolean {
    return this.busqueda?.motor === 'REGLAS_PROVISIONAL';
  }

  nivel(similitud: number): 'alta' | 'media' | 'baja' {
    if (similitud >= 70) return 'alta';
    if (similitud >= 40) return 'media';
    return 'baja';
  }

  etiquetaQueja(estatus?: string): string {
    return estatus ? ETIQUETAS_QUEJA[estatus] ?? estatus : 'Sin estatus';
  }

  abrirExpediente(antecedente: Antecedente): void {
    if (antecedente.folioPrimerContacto) {
      this.router.navigate(['/expediente', antecedente.folioPrimerContacto]);
    }
  }

  /**
   * Deja constancia en las notas del expediente de que el analista considera esta queja
   * como antecedente. Usa las notas normales, así queda con autor y fecha.
   */
  registrarComoAntecedente(antecedente: Antecedente): void {
    const contenido =
      `Antecedente relacionado: ${antecedente.folioQueja} (${antecedente.asunto ?? 'sin asunto'}, `
      + `${formatearFecha(antecedente.fecha)}). Similitud ${antecedente.similitud}% — `
      + `${antecedente.coincidencias.join('; ')}.`;

    this.notaService.crearNota({ folio: this.folio, contenido }).subscribe({
      next: () => {
        this.registrados.add(antecedente.folioQueja);
        this.cdr.detectChanges();
        this.snackBar.open('Antecedente registrado en las notas del expediente.', 'Cerrar', { duration: 3000 });
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible registrar el antecedente.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  volver(): void {
    this.router.navigate(['/expediente', this.folio]);
  }
}
