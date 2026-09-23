import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { EvidenciaResumen, QuejaDetalle } from '../../core/models/revision.models';

/** Requisitos que el recepcionista marca uno por uno junto al dato correspondiente. */
interface ChecksValidacion {
  datos: boolean;
  identificacion: boolean;
  motivo: boolean;
  relato: boolean;
  evidencias: boolean;
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

  /** Un check por cada dato de la queja; todos deben quedar marcados para canalizar. */
  checks: ChecksValidacion = {
    datos: false,
    identificacion: false,
    motivo: false,
    relato: false,
    evidencias: false,
  };
  readonly totalRequisitos = 5;

  /**
   * El relato de hechos se muestra difuminado por defecto: el recepcionista valida forma
   * (que exista y sea suficiente), no contenido. Si necesita leerlo lo revela a propósito.
   */
  mostrarRedaccion = false;

  /** Identificación del quejoso mostrada en grande en la columna derecha. Puede haber más de
   * una imagen (ej. credencial frente y reverso) -- se listan todas y se ve una a la vez. */
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
    // El object URL reserva memoria del navegador hasta que se revoca explícitamente.
    if (this.credencialObjectUrl) {
      URL.revokeObjectURL(this.credencialObjectUrl);
      this.credencialObjectUrl = null;
    }
  }

  get cumplidos(): number {
    return Object.values(this.checks).filter(Boolean).length;
  }

  get todosLosRequisitosCumplidos(): boolean {
    return this.cumplidos === this.totalRequisitos;
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
    if (!this.todosLosRequisitosCumplidos) {
      this.toast.advertencia(
        `Marca los ${this.totalRequisitos} requisitos antes de canalizar la queja.`,
      );
      return;
    }
    this.router.navigate(['/turnado', this.folio]);
  }

  irARechazar(): void {
    this.router.navigate(['/rechazo', this.folio]);
  }

  private cargar(): void {
    this.cargando = true;
    this.revisionService.detalle(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        this.cargarCredencial();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  /**
   * Reúne todas las imágenes entre las evidencias (credencial frente/reverso, capturas, etc.)
   * y baja la primera para mostrarla en grande. Si el quejoso solo subió PDFs no se muestra
   * nada: se abren desde la lista de evidencias.
   */
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

  /** Cambia cuál imagen se ve en grande (ej. pasar de frente a reverso de la credencial). */
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

    // El object URL de la imagen anterior ya no hace falta -- se libera antes de pedir la
    // siguiente, igual que al salir de la pantalla (ver ngOnDestroy).
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

  /** Formatea el tamaño de un archivo (bytes) para mostrarlo junto a cada evidencia. */
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

  /** Icono a mostrar en la lista de evidencias según el tipo de archivo. */
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
