import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { EvidenciaResumen, Queja, etiquetaEstatus } from '../../../core/models/queja.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-queja-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './queja-detalle.html',
  styleUrl: './queja-detalle.scss',
})
export class QuejaDetalle implements OnInit {
  folio = '';
  cargando = true;
  error = '';
  queja: Queja | null = null;
  evidencias: EvidenciaResumen[] = [];

  constructor(
    private route: ActivatedRoute,
    private quejaService: QuejaService,
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
  }

  get estatus(): string {
    return etiquetaEstatus(this.queja?.estatus);
  }

  get puedeEditar(): boolean {
    return this.estatus === 'Recibida';
  }

  formatearTamanio(bytes?: number): string {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  avisarEdicionPendiente(): void {
    this.toast.advertencia(
      'La edición de quejas está en desarrollo. Por ahora solo puedes consultar el detalle.',
    );
  }
}
