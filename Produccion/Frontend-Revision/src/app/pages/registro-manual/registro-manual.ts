import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { AreaOpcion } from '../../core/models/revision.models';

@Component({
  selector: 'app-registro-manual',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro-manual.html',
  styleUrl: './registro-manual.scss',
})
export class RegistroManual implements OnInit {
  areas: AreaOpcion[] = [];
  guardando = false;
  archivoSeleccionado: File | null = null;
  nombreArchivo = '';

  formulario = this.formularioVacio();

  constructor(
    private revisionService: RevisionService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.revisionService.areas().subscribe({
      next: (areas) => {
        this.areas = areas;
        this.cdr.detectChanges();
      },
      error: () => {
        // El formulario sigue siendo usable sin el catálogo -- solo no habrá opciones en el combo.
      },
    });
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    this.archivoSeleccionado = archivo;
    this.nombreArchivo = archivo?.name ?? '';
  }

  limpiarFormulario(): void {
    this.formulario = this.formularioVacio();
    this.archivoSeleccionado = null;
    this.nombreArchivo = '';
  }

  guardar(): void {
    if (!this.formulario.nombre || !this.formulario.apellidoPaterno || !this.formulario.descripcion) {
      this.toast.advertencia('Completa al menos nombre, primer apellido y descripción del asunto.');
      return;
    }

    const datos = new FormData();
    datos.append('nombre', this.formulario.nombre);
    datos.append('apellidoPaterno', this.formulario.apellidoPaterno);
    if (this.formulario.apellidoMaterno) datos.append('apellidoMaterno', this.formulario.apellidoMaterno);
    if (this.formulario.tipoUsuario) datos.append('tipoUsuario', this.formulario.tipoUsuario);
    if (this.formulario.dependenciaClave) datos.append('dependenciaClave', this.formulario.dependenciaClave);
    if (this.formulario.numeroOficio) datos.append('numeroOficio', this.formulario.numeroOficio);
    if (this.formulario.fechaRecepcionFisica) datos.append('fechaRecepcionFisica', this.formulario.fechaRecepcionFisica);
    if (this.formulario.tipoDocumento) datos.append('tipoDocumento', this.formulario.tipoDocumento);
    datos.append('descripcion', this.formulario.descripcion);
    if (this.formulario.ubicacionFisica) datos.append('ubicacionFisica', this.formulario.ubicacionFisica);
    if (this.archivoSeleccionado) datos.append('archivo', this.archivoSeleccionado);

    this.guardando = true;
    this.revisionService.registrarManual(datos).subscribe({
      next: (resp) => {
        this.guardando = false;
        this.toast.exito(resp.mensaje);
        this.limpiarFormulario();
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.guardando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo registrar la entrada.');
        this.cdr.detectChanges();
      },
    });
  }

  private formularioVacio() {
    return {
      nombre: '',
      apellidoPaterno: '',
      apellidoMaterno: '',
      tipoUsuario: '',
      dependenciaClave: '',
      numeroOficio: '',
      fechaRecepcionFisica: '',
      tipoDocumento: '',
      descripcion: '',
      ubicacionFisica: '',
    };
  }
}
