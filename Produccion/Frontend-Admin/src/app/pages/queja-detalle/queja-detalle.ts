import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { DetalleQuejaSeg } from '../../core/models/admin.models';

/** Expediente de una queja para la Defensora: todo a la vista, sin poder modificar nada. */
@Component({
  selector: 'app-queja-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './queja-detalle.html',
  styleUrl: './queja-detalle.scss',
})
export class QuejaDetalle implements OnInit {
  queja: DetalleQuejaSeg | null = null;
  cargando = true;
  abriendo: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private seguimiento: SeguimientoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const folio = this.route.snapshot.paramMap.get('folio') ?? '';
    this.seguimiento.detalle(folio).subscribe({
      next: (q) => {
        this.queja = q;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  regresar(): void {
    this.location.back();
  }

  /** El archivo exige el token, así que se descarga como blob y se abre en otra pestaña. */
  verArchivo(id: number): void {
    if (!this.queja) {
      return;
    }
    this.abriendo = id;
    this.seguimiento.evidencia(this.queja.folio, id).subscribe({
      next: (blob) => {
        this.abriendo = null;
        window.open(URL.createObjectURL(blob), '_blank');
        this.cdr.detectChanges();
      },
      error: () => {
        this.abriendo = null;
        this.toast.error('No se pudo abrir el archivo.');
        this.cdr.detectChanges();
      },
    });
  }

  peso(bytes?: number | null): string {
    if (!bytes) {
      return '';
    }
    return bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  /** Motivos (antes de la línea en blanco) y observaciones detalladas del rechazo. */
  get motivos(): string[] {
    return (this.queja?.motivoRechazo ?? '').split('\n\n')[0].split(';').map((m) => m.trim()).filter(Boolean);
  }

  get observaciones(): string {
    const partes = (this.queja?.motivoRechazo ?? '').split('\n\n');
    return partes.slice(1).join('\n\n').trim();
  }
}
