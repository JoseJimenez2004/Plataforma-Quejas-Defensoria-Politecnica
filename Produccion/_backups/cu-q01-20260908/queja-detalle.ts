import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { EvidenciaResumen, Queja, etiquetaEstatus } from '../../../core/models/queja.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { ToastService } from '../../../core/services/toast.service';
import { Datepicker } from '../../../shared/datepicker/datepicker';

@Component({
  selector: 'app-queja-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Datepicker],
  templateUrl: './queja-detalle.html',
  styleUrl: './queja-detalle.scss',
})
export class QuejaDetalle implements OnInit {
  folio = '';
  cargando = true;
  error = '';
  queja: Queja | null = null;
  evidencias: EvidenciaResumen[] = [];
  dependencias: Dependencia[] = [];

  editando = false;
  guardando = false;
  readonly fechaMaxima = new Date().toISOString().split('T')[0];

  // Campos del formulario de edición (copia de trabajo, no se toca "queja" hasta guardar).
  formDescripcion = '';
  formUnidadAcademica = '';
  formFechaHechos = '';
  formNombreDenunciado = '';
  formApellidoDenunciado = '';

  constructor(
    private route: ActivatedRoute,
    private quejaService: QuejaService,
    private catalogoService: CatalogoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    if (!this.folio) return;

    this.quejaService.miQuejaPorFolio(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudo cargar el detalle de la queja.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });

    this.quejaService.misEvidencias(this.folio).subscribe({
      next: (evidencias) => {
        this.evidencias = evidencias;
        this.cdr.detectChanges();
      },
      error: () => {
        // No es crítico para ver el detalle si esto falla — simplemente no se muestran.
      },
    });

    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  get estatus(): string {
    return etiquetaEstatus(this.queja?.estatus);
  }

  get puedeEditar(): boolean {
    return this.estatus === 'Recibida';
  }

  nombreUnidad(clave?: string): string {
    if (!clave) return '—';
    return this.dependencias.find((d) => d.clave === clave)?.nombre ?? clave;
  }

  formatearTamanio(bytes?: number): string {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  iniciarEdicion(): void {
    if (!this.queja) return;
    this.formDescripcion = this.queja.descripcion ?? '';
    this.formUnidadAcademica = this.queja.unidadAcademicaClave ?? '';
    this.formFechaHechos = this.queja.fechaHechos ?? '';
    this.formNombreDenunciado = this.queja.nombreDenunciado ?? '';
    this.formApellidoDenunciado = this.queja.apellidoDenunciado ?? '';
    this.editando = true;
  }

  cancelarEdicion(): void {
    this.editando = false;
  }

  guardarEdicion(): void {
    if (!this.folio || !this.formDescripcion.trim()) {
      this.toast.advertencia('La descripción de los hechos no puede quedar vacía.');
      return;
    }

    this.guardando = true;
    this.quejaService
      .editarMiQueja(this.folio, {
        descripcion: this.formDescripcion,
        unidadAcademicaClave: this.formUnidadAcademica || undefined,
        fechaHechos: this.formFechaHechos || undefined,
        nombreDenunciado: this.formNombreDenunciado || undefined,
        apellidoDenunciado: this.formApellidoDenunciado || undefined,
      })
      .subscribe({
        next: (quejaActualizada) => {
          this.guardando = false;
          this.queja = quejaActualizada;
          this.editando = false;
          this.toast.exito('Los cambios de tu queja se guardaron correctamente.');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.guardando = false;
          const mensaje = err?.error?.mensaje ?? 'No se pudo guardar la edición. Intenta de nuevo.';
          this.toast.error(mensaje);
          this.cdr.detectChanges();
        },
      });
  }
}
