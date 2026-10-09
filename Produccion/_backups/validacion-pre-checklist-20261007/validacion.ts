import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { EvidenciaResumen, IdentificacionOficialOpcion, QuejaDetalle } from '../../core/models/revision.models';

/** Respuestas de validación del recepcionista. Solo habilitan/deshabilitan el turnado. */
interface ChecksValidacion {
  nombreCompleto: boolean | null;
  identificacionOficial: boolean | null;
}

@Component({
  selector: 'app-validacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './validacion.html',
  styleUrl: './validacion.scss',
})
export class Validacion implements OnInit, OnDestroy {
  folio = '';
  queja: QuejaDetalle | null = null;
  cargando = true;

  checks: ChecksValidacion = {
    nombreCompleto: null,
    identificacionOficial: null,
  };
  readonly totalRequisitos = 2;

  observaciones = '';
  identificaciones: IdentificacionOficialOpcion[] = [];
  tipoIdentificacionPresentada = '';
  cargandoIdentificaciones = false;

  mostrarRedaccion = false;

  imagenesCredencial: EvidenciaResumen[] = [];
  indiceCredencial = 0;
  credencialUrl: SafeUrl | null = null;
  credencialNombre = '';
  credencialId: number | null = null;
  cargandoCredencial = false;
  private credencialObjectUrl: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private revisionService: RevisionService,
    private toast: ToastService,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.credencialObjectUrl) {
      URL.revokeObjectURL(this.credencialObjectUrl);
      this.credencialObjectUrl = null;
    }
  }

  get cumplidos(): number {
    let total = 0;
    if (this.checks.nombreCompleto === true) total++;
    if (this.checks.identificacionOficial === true) total++;
    return total;
  }

  get puedeCanalizar(): boolean {
    return this.checks.nombreCompleto === true && this.checks.identificacionOficial === true;
  }

  get puedeRegresar(): boolean {
    return (
      (this.checks.nombreCompleto === false || this.checks.identificacionOficial === false) &&
      this.observaciones.trim().length > 0
    );
  }

  setRespuesta(campo: keyof ChecksValidacion, valor: boolean): void {
    this.checks[campo] = valor;
    this.cdr.detectChanges();
  }

  alternarRedaccion(): void {
    this.mostrarRedaccion = !this.mostrarRedaccion;
  }

  verDocumento(id: number): void {
    this.revisionService.descargarEvidencia(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => this.toast.error('No se pudo abrir el documento.'),
    });
  }

  irACanalizar(): void {
    if (!this.puedeCanalizar) {
      this.toast.advertencia('Marca ambas validaciones como "Sí" antes de canalizar la queja.');
      return;
    }
    this.router.navigate(['/turnado', this.folio]);
  }

  regresarAlQuejoso(): void {
    if (!this.puedeRegresar) {
      this.toast.advertencia('Indica qué dato no es válido y escribe las observaciones para el quejoso.');
      return;
    }

    const motivos = ['Datos o identificación no válidos'];
    this.revisionService.rechazar(this.folio, { motivos, observaciones: this.observaciones }).subscribe({
      next: () => {
        this.toast.exito('La queja fue regresada al quejoso con observaciones.');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo regresar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  private cargar(): void {
    this.cargando = true;
    this.revisionService.detalle(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        this.cargarCredencial();
        this.cargarIdentificaciones();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  private cargarIdentificaciones(): void {
    this.cargandoIdentificaciones = true;
    this.revisionService.identificacionesOficiales().subscribe({
      next: (lista) => {
        this.identificaciones = lista;
        this.cargandoIdentificaciones = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoIdentificaciones = false;
        this.identificaciones = [];
        this.cdr.detectChanges();
      },
    });
  }

  private cargarCredencial(): void {
    this.imagenesCredencial = (this.queja?.evidencias ?? []).filter((ev) =>
      (ev.tipoMime ?? '').toLowerCase().startsWith('image/'),
    );
    this.indiceCredencial = 0;

    if (this.imagenesCredencial.length === 0) {
      return;
    }

    this.mostrarCredencial(0);
  }

  seleccionarCredencial(indice: number): void {
    if (indice === this.indiceCredencial || indice < 0 || indice >= this.imagenesCredencial.length) {
      return;
    }
    this.mostrarCredencial(indice);
  }

  private mostrarCredencial(indice: number): void {
    const imagen = this.imagenesCredencial[indice];
    if (!imagen) {
      return;
    }

    this.indiceCredencial = indice;
    this.credencialId = imagen.id;
    this.credencialNombre = imagen.nombreArchivo;
    this.cargandoCredencial = true;

    if (this.credencialObjectUrl) {
      URL.revokeObjectURL(this.credencialObjectUrl);
      this.credencialObjectUrl = null;
      this.credencialUrl = null;
    }

    this.revisionService.descargarEvidencia(imagen.id).subscribe({
      next: (blob) => {
        this.credencialObjectUrl = URL.createObjectURL(blob);
        this.credencialUrl = this.sanitizer.bypassSecurityTrustUrl(this.credencialObjectUrl);
        this.cargandoCredencial = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoCredencial = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatearTamanio(bytes: number): string {
    if (!bytes || bytes <= 0) {
      return '';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    const kb = bytes / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(kb < 10 ? 1 : 0)} KB`;
    }
    return `${(kb / 1024).toFixed(1)} MB`;
  }

  tipoIcono(ev: EvidenciaResumen): 'imagen' | 'pdf' | 'archivo' {
    const tipo = (ev.tipoMime ?? '').toLowerCase();
    if (tipo.startsWith('image/')) {
      return 'imagen';
    }
    if (tipo === 'application/pdf') {
      return 'pdf';
    }
    return 'archivo';
  }
}
