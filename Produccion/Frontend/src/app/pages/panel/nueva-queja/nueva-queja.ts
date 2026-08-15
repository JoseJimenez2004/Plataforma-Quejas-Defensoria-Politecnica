import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { Queja } from '../../../core/models/queja.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { NotaFlotante } from '../../../shared/nota-flotante/nota-flotante';
import { Datepicker } from '../../../shared/datepicker/datepicker';

@Component({
  selector: 'app-nueva-queja',
  standalone: true,
  imports: [CommonModule, FormsModule, NotaFlotante, Datepicker],
  templateUrl: './nueva-queja.html',
  styleUrl: './nueva-queja.scss',
})
export class NuevaQueja implements OnInit {
  unidadAcademica = '';
  fechaHechos = '';
  readonly fechaMaxima = new Date().toISOString().split('T')[0];
  nombreDenunciado = '';
  apellidoDenunciado = '';
  relato = '';
  archivos: File[] = [];
  /** Igual que en el registro público: se manda como evidencia renombrada con el prefijo
   * IDENTIFICACION_ (el backend no distingue "tipos" de evidencia todavía). */
  identificacionArchivo: File | null = null;

  /** Controla el estado visual del dropzone mientras se arrastra un archivo encima (mismo
   * patrón que registro-queja-publico). */
  arrastrandoArchivo = false;

  dependencias: Dependencia[] = [];
  cargandoDependencias = true;

  cargando = false;
  error = '';
  quejaCreada: Queja | null = null;

  constructor(
    private quejaService: QuejaService,
    private catalogoService: CatalogoService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cargandoDependencias = false;
      },
      error: () => {
        this.cargandoDependencias = false;
      },
    });
  }

  onArchivosSeleccionados(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    for (const archivo of Array.from(input.files)) {
      this.archivos.push(archivo);
    }
    input.value = '';
  }

  quitarArchivo(indice: number): void {
    this.archivos.splice(indice, 1);
  }

  onArrastreSobre(event: DragEvent): void {
    event.preventDefault();
    this.arrastrandoArchivo = true;
  }

  onArrastreSale(): void {
    this.arrastrandoArchivo = false;
  }

  onArchivosSoltados(event: DragEvent): void {
    event.preventDefault();
    this.arrastrandoArchivo = false;
    if (!event.dataTransfer?.files) return;
    for (const archivo of Array.from(event.dataTransfer.files)) {
      this.archivos.push(archivo);
    }
  }

  formatearTamanio(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  onIdentificacionSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;

    const extension = '.' + (archivo.name.split('.').pop()?.toLowerCase() ?? '');
    if (!['.pdf', '.jpg', '.jpeg', '.png'].includes(extension)) {
      this.error = 'La identificación debe ser un archivo PDF, JPG o PNG.';
      return;
    }
    this.identificacionArchivo = archivo;
  }

  quitarIdentificacion(): void {
    this.identificacionArchivo = null;
  }

  enviar(): void {
    this.error = '';

    if (!this.unidadAcademica || !this.relato) {
      this.error = 'Completa al menos la unidad académica y el relato de los hechos.';
      return;
    }

    if (!this.identificacionArchivo) {
      this.error = 'Adjunta tu identificación oficial de la comunidad politécnica.';
      return;
    }

    // Antes se combinaba todo a mano dentro de "descripcion" porque el backend no tenía
    // columnas propias para unidad académica/fecha/denunciado — ya se mandan como campos
    // estructurados y el backend los guarda en sus propias columnas (ver docs/CAMBIOS.md).
    const motivo = `Queja en ${this.unidadAcademica}`;

    const archivoIdentificacion = new File(
      [this.identificacionArchivo],
      `IDENTIFICACION_${this.identificacionArchivo.name}`,
      { type: this.identificacionArchivo.type },
    );

    this.cargando = true;
    this.quejaService
      .registrarQueja({
        motivo,
        descripcion: this.relato,
        unidadAcademicaClave: this.unidadAcademica,
        fechaHechos: this.fechaHechos || undefined,
        nombreDenunciado: this.nombreDenunciado || undefined,
        apellidoDenunciado: this.apellidoDenunciado || undefined,
        archivos: [archivoIdentificacion, ...this.archivos],
      })
      .subscribe({
        next: (queja) => {
          this.cargando = false;
          this.quejaCreada = queja;
        },
        error: (err) => {
          this.cargando = false;
          this.error = err?.error?.mensaje ?? 'No se pudo registrar la queja. Intenta de nuevo.';
        },
      });
  }

  volverAlTablero(): void {
    this.router.navigate(['/panel']);
  }
}
