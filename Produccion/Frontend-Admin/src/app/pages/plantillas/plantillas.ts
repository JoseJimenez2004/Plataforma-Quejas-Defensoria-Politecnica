import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PlantillaService } from '../../core/services/plantilla.service';
import { ToastService } from '../../core/services/toast.service';
import { PlantillaDocumento } from '../../core/models/admin.models';

@Component({
  selector: 'app-plantillas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plantillas.html',
  styleUrl: './plantillas.scss',
})
export class Plantillas implements OnInit {
  lista: PlantillaDocumento[] = [];
  placeholders: Record<string, string> = {};
  cargando = true;
  guardando = false;

  tipoSeleccionado = '';
  contenido = '';

  mostrarPrevisualizacion = false;
  textoPrevisualizacion = '';

  constructor(
    private plantillaService: PlantillaService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.plantillaService.placeholders().subscribe({
      next: (p) => (this.placeholders = p),
    });

    this.plantillaService.listar().subscribe({
      next: (lista) => {
        this.lista = lista;
        this.cargando = false;
        if (lista.length) {
          this.seleccionar(lista[0].tipo);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudieron cargar las plantillas.');
        this.cdr.detectChanges();
      },
    });
  }

  seleccionar(tipo: string): void {
    this.tipoSeleccionado = tipo;
    const plantilla = this.lista.find((p) => p.tipo === tipo);
    this.contenido = plantilla?.contenido ?? '';
  }

  onCambioSelector(event: Event): void {
    const tipo = (event.target as HTMLSelectElement).value;
    this.seleccionar(tipo);
  }

  guardar(): void {
    if (!this.tipoSeleccionado) return;
    this.guardando = true;
    this.plantillaService.actualizar(this.tipoSeleccionado, this.contenido).subscribe({
      next: (actualizada) => {
        this.guardando = false;
        const indice = this.lista.findIndex((p) => p.tipo === actualizada.tipo);
        if (indice >= 0) this.lista[indice] = actualizada;
        this.toast.exito('Plantilla guardada y publicada.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardando = false;
        this.toast.error('No se pudo guardar la plantilla.');
        this.cdr.detectChanges();
      },
    });
  }

  verPrevisualizacion(): void {
    if (!this.tipoSeleccionado) return;
    this.plantillaService.previsualizar(this.tipoSeleccionado).subscribe({
      next: (texto) => {
        this.textoPrevisualizacion = texto;
        this.mostrarPrevisualizacion = true;
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('No se pudo generar la previsualización.'),
    });
  }

  cerrarPrevisualizacion(): void {
    this.mostrarPrevisualizacion = false;
  }

  get listaPlaceholders(): string[] {
    return Object.keys(this.placeholders).map((clave) => `{${clave}} — ${this.placeholders[clave]}`);
  }
}
