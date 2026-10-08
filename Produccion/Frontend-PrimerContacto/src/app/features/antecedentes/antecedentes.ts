import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  Antecedente,
  AntecedenteGuardado,
  BusquedaAntecedentes,
  FuenteAntecedente,
  claveAntecedente
} from '../../core/models/antecedentes.model';
import { ExpedienteDetalle } from '../../core/models/expediente-detalle';
import { AntecedentesService } from '../../core/services/antecedentes.service';
import { ExpedienteService } from '../../core/services/expediente.service';
import { formatearFecha } from '../../core/utils/estatus-expediente';
import { mensajeDeError } from '../../core/utils/archivos';
import { ResumenDialog, ResumenDialogData } from '../../shared/resumen-dialog/resumen-dialog';
import {
  AntecedenteDetalleData,
  AntecedenteDetalleDialog
} from '../../shared/antecedente-detalle-dialog/antecedente-detalle-dialog';

/** Estatus de quejas de cualquier etapa (recepción, primer contacto e histórico). */
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
  REMITIDA: 'Remitida',
  CONCLUIDA: 'Concluida'
};

const ETIQUETAS_FUENTE: Record<FuenteAntecedente, string> = {
  MANUAL: 'Búsqueda manual',
  MODELO: 'Modelo',
  REGLAS_PROVISIONAL: 'Motor provisional'
};

/** Un antecedente marcado en pantalla, con la búsqueda de la que salió. */
interface Seleccionado {
  antecedente: Antecedente;
  fuente: FuenteAntecedente;
}

/**
 * Antecedentes de la queja que se analiza, con dos búsquedas que el analista combina:
 *
 *   - Manual: por nombre del quejoso y/o del denunciado (criterio humano), en las quejas del
 *     sistema y en los casos históricos.
 *   - Con el modelo: quejas con narrativa parecida (si el modelo no responde, el backend usa
 *     el motor provisional por reglas y lo avisa).
 *
 * Lo que se marca en una pestaña sigue marcado en la otra (la selección es por origen +
 * folio), y "Guardar seleccionados" guarda todo junto como antecedentes finales.
 */
@Component({
  selector: 'app-antecedentes',
  imports: [
    NgTemplateOutlet,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatButtonToggleModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatTabsModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './antecedentes.html',
  styleUrl: './antecedentes.css'
})
export class Antecedentes implements OnInit {
  folio = '';

  expediente?: ExpedienteDetalle;

  // ---- Pestaña manual ----
  nombreQuejoso = '';
  nombreDenunciado = '';
  manual?: BusquedaAntecedentes;
  buscandoManual = false;
  errorManual = '';

  // ---- Pestaña del modelo ----
  modelo?: BusquedaAntecedentes;
  buscandoModelo = false;
  errorModelo = false;
  soloMismoQuejoso = false;
  similitudMinima = 0;

  // ---- Selección y guardados ----
  /** Marcados en cualquiera de las dos pestañas, por clave origen:folio. */
  seleccion = new Map<string, Seleccionado>();
  guardados: AntecedenteGuardado[] = [];
  guardando = false;

  readonly formatearFecha = formatearFecha;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private antecedentesService: AntecedentesService,
    private expedienteService: ExpedienteService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.folio = this.route.snapshot.paramMap.get('id') ?? '';
  }

  ngOnInit(): void {
    this.expedienteService.obtenerPorFolio(this.folio).subscribe({
      next: (expediente) => {
        this.expediente = this.expedienteService.mapearADetalle(expediente);
        this.cdr.detectChanges();
      }
    });

    this.cargarGuardados();
    this.buscarConModelo();
  }

  // ------------------------------------------------------------------ Búsqueda manual

  usarQuejosoDeLaQueja(): void {
    this.nombreQuejoso = this.expediente?.quejoso?.nombre ?? '';
  }

  buscarManual(): void {
    if (!this.nombreQuejoso.trim() && !this.nombreDenunciado.trim()) {
      this.errorManual = 'Escribe el nombre del quejoso, del denunciado o de ambos.';
      return;
    }

    this.buscandoManual = true;
    this.errorManual = '';
    this.cdr.detectChanges();

    this.antecedentesService.buscarManual(this.folio, this.nombreQuejoso, this.nombreDenunciado).subscribe({
      next: (busqueda) => {
        this.manual = busqueda;
        this.buscandoManual = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.buscandoManual = false;
        this.errorManual = mensajeDeError(error, 'No fue posible completar la búsqueda.');
        this.cdr.detectChanges();
      }
    });
  }

  // ------------------------------------------------------------------ Búsqueda con modelo

  buscarConModelo(): void {
    this.buscandoModelo = true;
    this.errorModelo = false;
    this.cdr.detectChanges();

    this.antecedentesService.buscar(this.folio).subscribe({
      next: (busqueda) => {
        this.modelo = busqueda;
        this.buscandoModelo = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.buscandoModelo = false;
        this.errorModelo = true;
        this.cdr.detectChanges();
      }
    });
  }

  get resultadosModelo(): Antecedente[] {
    return (this.modelo?.resultados ?? []).filter(a =>
      (!this.soloMismoQuejoso || a.mismoQuejoso) && a.similitud >= this.similitudMinima
    );
  }

  get totalMismoQuejoso(): number {
    return (this.modelo?.resultados ?? []).filter(a => a.mismoQuejoso).length;
  }

  get esProvisional(): boolean {
    return this.modelo?.motor === 'REGLAS_PROVISIONAL';
  }

  // ------------------------------------------------------------------ Selección

  clave(antecedente: Antecedente): string {
    return claveAntecedente(antecedente);
  }

  estaSeleccionado(antecedente: Antecedente): boolean {
    return this.seleccion.has(this.clave(antecedente));
  }

  estaGuardado(antecedente: Antecedente): boolean {
    const clave = this.clave(antecedente);
    return this.guardados.some(g => claveAntecedente(g) === clave);
  }

  alternarSeleccion(antecedente: Antecedente, fuente: FuenteAntecedente): void {
    const clave = this.clave(antecedente);
    if (this.seleccion.has(clave)) {
      this.seleccion.delete(clave);
    } else {
      this.seleccion.set(clave, { antecedente, fuente });
    }
  }

  limpiarSeleccion(): void {
    this.seleccion.clear();
  }

  guardarSeleccion(): void {
    if (this.seleccion.size === 0) return;

    const items: AntecedenteGuardado[] = [...this.seleccion.values()].map(({ antecedente, fuente }) => ({
      origen: antecedente.origen,
      folioQueja: antecedente.folioQueja,
      fuente,
      similitud: fuente === 'MANUAL' ? null : antecedente.similitud,
      asunto: antecedente.asunto,
      fecha: antecedente.fecha,
      nombreQuejoso: antecedente.nombreQuejoso,
      nombreDenunciado: antecedente.nombreDenunciado,
      unidadAcademica: antecedente.unidadAcademica,
      estatus: antecedente.estatus,
      folioPrimerContacto: antecedente.folioPrimerContacto,
      extracto: antecedente.extracto,
      descripcion: antecedente.descripcion,
      resultado: antecedente.resultado
    }));

    this.guardando = true;
    this.antecedentesService.guardar(this.folio, items).subscribe({
      next: (guardados) => {
        this.guardados = guardados;
        this.guardando = false;
        const total = this.seleccion.size;
        this.seleccion.clear();
        this.cdr.detectChanges();
        this.snackBar.open(
          `${total} antecedente${total === 1 ? '' : 's'} guardado${total === 1 ? '' : 's'} en el expediente.`,
          'Cerrar', { duration: 3000 });
      },
      error: (error) => {
        this.guardando = false;
        this.cdr.detectChanges();
        this.snackBar.open(mensajeDeError(error, 'No fue posible guardar los antecedentes.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  quitarGuardado(guardado: AntecedenteGuardado): void {
    if (!guardado.id) return;
    if (!confirm(`¿Quitar ${guardado.folioQueja} de los antecedentes del expediente?`)) return;

    this.antecedentesService.quitar(this.folio, guardado.id).subscribe({
      next: () => {
        this.guardados = this.guardados.filter(g => g.id !== guardado.id);
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible quitar el antecedente.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }

  private cargarGuardados(): void {
    this.antecedentesService.guardados(this.folio).subscribe({
      next: (guardados) => {
        this.guardados = guardados;
        this.cdr.detectChanges();
      }
    });
  }

  // ------------------------------------------------------------------ Resumen

  verResumenQuejaAnalizada(): void {
    this.abrirResumen({
      folio: this.expediente?.folioOrigen ?? this.folio,
      asunto: this.expediente?.asunto,
      texto: this.expediente?.narrativa
    });
  }

  verResumen(antecedente: Antecedente): void {
    this.abrirResumen({
      folio: antecedente.folioQueja,
      asunto: antecedente.asunto,
      texto: antecedente.descripcion ?? antecedente.extracto
    });
  }

  /** Detalle completo de un antecedente (resultado o guardado), con lo ya cargado. */
  verDetalle(antecedente: AntecedenteDetalleData): void {
    this.dialog.open(AntecedenteDetalleDialog, { width: '720px', data: antecedente });
  }

  private abrirResumen(data: ResumenDialogData): void {
    this.dialog.open(ResumenDialog, { width: '640px', data });
  }

  // ------------------------------------------------------------------ Presentación

  nivel(similitud: number): 'alta' | 'media' | 'baja' {
    if (similitud >= 70) return 'alta';
    if (similitud >= 40) return 'media';
    return 'baja';
  }

  etiquetaQueja(estatus?: string): string {
    return estatus ? ETIQUETAS_QUEJA[estatus] ?? estatus : 'Sin estatus';
  }

  etiquetaOrigen(origen: string): string {
    return origen === 'HISTORICO' ? 'Histórico' : 'Sistema';
  }

  etiquetaFuente(fuente: FuenteAntecedente): string {
    return ETIQUETAS_FUENTE[fuente] ?? fuente;
  }

  abrirExpediente(folioPrimerContacto?: string): void {
    if (folioPrimerContacto) {
      this.router.navigate(['/expediente', folioPrimerContacto]);
    }
  }

  volver(): void {
    this.router.navigate(['/expediente', this.folio]);
  }
}
