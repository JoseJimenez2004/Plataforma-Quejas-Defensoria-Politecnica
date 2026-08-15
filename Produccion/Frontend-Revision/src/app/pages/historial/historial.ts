import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { HistorialService } from '../../core/services/historial.service';
import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { HistorialItem, QuejaDetalle } from '../../core/models/revision.models';

@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historial.html',
  styleUrl: './historial.scss',
})
export class Historial implements OnInit {
  items: HistorialItem[] = [];
  cargando = true;
  exportando = false;

  filtroTexto = '';
  filtroEstatus = '';
  filtroFecha = '';

  mostrarModalDetalle = false;
  detalleSeleccionado: QuejaDetalle | null = null;

  constructor(
    private historialService: HistorialService,
    private revisionService: RevisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    this.cargando = true;
    this.historialService.listar(this.filtroTexto, this.filtroEstatus, this.filtroFecha).subscribe({
      next: (items) => {
        this.items = items;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el historial.');
        this.cdr.detectChanges();
      },
    });
  }

  verDetalle(folio: string): void {
    this.revisionService.detalle(folio).subscribe({
      next: (detalle) => {
        this.detalleSeleccionado = detalle;
        this.mostrarModalDetalle = true;
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('No se pudo cargar el detalle de la queja.'),
    });
  }

  cerrarDetalle(): void {
    this.mostrarModalDetalle = false;
    this.detalleSeleccionado = null;
  }

  exportar(): void {
    this.exportando = true;
    this.historialService.exportar(this.filtroTexto, this.filtroEstatus, this.filtroFecha).subscribe({
      next: (blob) => {
        this.exportando = false;
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'historial-tramites.xlsx';
        enlace.click();
        URL.revokeObjectURL(url);
        this.cdr.detectChanges();
      },
      error: () => {
        this.exportando = false;
        this.toast.error('No se pudo exportar el reporte.');
        this.cdr.detectChanges();
      },
    });
  }
}
