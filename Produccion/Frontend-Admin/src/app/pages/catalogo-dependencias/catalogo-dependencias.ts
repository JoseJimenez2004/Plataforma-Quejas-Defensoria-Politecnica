import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CatalogoAdminService,
  DependenciaRequest,
} from '../../core/services/catalogo-admin.service';
import { ToastService } from '../../core/services/toast.service';
import { Dependencia } from '../../core/models/admin.models';

@Component({
  selector: 'app-catalogo-dependencias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-dependencias.html',
  styleUrl: './catalogo-dependencias.scss',
})
export class CatalogoDependencias implements OnInit {
  lista: Dependencia[] = [];
  cargando = true;
  importando = false;
  busqueda = '';

  mostrarModal = false;
  editandoClave: string | null = null;
  formulario: DependenciaRequest = this.formularioVacio();

  constructor(
    private catalogoService: CatalogoAdminService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarLista();
  }

  get listaFiltrada(): Dependencia[] {
    const termino = this.busqueda.trim().toLowerCase();
    if (!termino) return this.lista;
    return this.lista.filter((d) =>
      d.nombre.toLowerCase().includes(termino) ||
      (d.abreviatura ?? '').toLowerCase().includes(termino) ||
      d.clave.toLowerCase().includes(termino) ||
      (d.correoContacto ?? '').toLowerCase().includes(termino),
    );
  }

  private cargarLista(): void {
    this.cargando = true;
    this.catalogoService.listarTodas().subscribe({
      next: (lista) => {
        this.lista = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el catálogo de dependencias.');
        this.cdr.detectChanges();
      },
    });
  }

  abrirCrear(): void {
    this.editandoClave = null;
    this.formulario = this.formularioVacio();
    this.mostrarModal = true;
  }

  abrirEditar(dep: Dependencia): void {
    this.editandoClave = dep.clave;
    this.formulario = {
      clave: dep.clave,
      clavePadre: dep.clavePadre,
      nombre: dep.nombre,
      abreviatura: dep.abreviatura,
      tipo: dep.tipo,
      categoria: dep.categoria,
      nivel: dep.nivel,
      correoContacto: dep.correoContacto,
      nombreTitular: dep.nombreTitular,
    };
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.editandoClave = null;
  }

  guardar(): void {
    if (!this.formulario.clave || !this.formulario.nombre || !this.formulario.tipo) {
      this.toast.advertencia('Completa clave, nombre y tipo.');
      return;
    }

    const accion = this.editandoClave
      ? this.catalogoService.editar(this.editandoClave, this.formulario)
      : this.catalogoService.crear(this.formulario);

    accion.subscribe({
      next: () => {
        this.mostrarModal = false;
        this.toast.exito(this.editandoClave ? 'Dependencia actualizada.' : 'Dependencia creada.');
        this.cargarLista();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo guardar la dependencia.');
        this.cdr.detectChanges();
      },
    });
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;

    this.importando = true;
    this.catalogoService.importarExcel(archivo).subscribe({
      next: (resumen) => {
        this.importando = false;
        this.toast.exito(
          `Importación completa: ${resumen.filasCreadas} creadas, ${resumen.filasActualizadas} actualizadas.`,
        );
        if (resumen.errores.length) {
          this.toast.advertencia(`${resumen.errores.length} filas con errores, revisa el formato del archivo.`);
        }
        this.cargarLista();
        this.cdr.detectChanges();
        input.value = '';
      },
      error: (err) => {
        this.importando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo importar el archivo.');
        this.cdr.detectChanges();
        input.value = '';
      },
    });
  }

  private formularioVacio(): DependenciaRequest {
    return { clave: '', nombre: '', tipo: 'UNIDAD_ACADEMICA', nivel: 1 };
  }
}
