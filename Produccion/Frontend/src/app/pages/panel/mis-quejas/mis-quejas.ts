import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { QuejaService } from '../../../core/services/queja.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import {
  Queja,
  claseEstatus,
  esEditable,
  estaCerrada,
  etiquetaEstatus,
} from '../../../core/models/queja.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { ToastService } from '../../../core/services/toast.service';
import { ICONOS } from '../../../shared/iconos/iconos';

/** Agrupación por la que puede filtrar el quejoso. */
type Pestania = 'todas' | 'tramite' | 'cerradas';

@Component({
  selector: 'app-mis-quejas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './mis-quejas.html',
  styleUrl: './mis-quejas.scss',
})
export class MisQuejas implements OnInit {
  readonly ICONOS = ICONOS;

  /**
   * Un solo cuadro de búsqueda en vez de los cinco filtros anteriores.
   *
   * Antes había folio, asunto, unidad académica, fecha y estatus — cinco campos para una
   * lista que en la práctica tiene entre una y cinco quejas. El filtro de unidad académica
   * casi nunca servía (un quejoso se queja de su propia escuela), y filtrar por fecha exacta
   * con tan pocos renglones tampoco. Ninguna información se perdió: sigue toda en la tabla.
   */
  busqueda = '';
  pestania: Pestania = 'todas';

  quejas: Queja[] = [];
  dependencias: Dependencia[] = [];
  cargando = true;
  error = '';

  /** Folio de la queja que el usuario quiere retirar; abre el diálogo de confirmación. */
  folioPorCancelar: string | null = null;
  cancelando = false;

  constructor(
    private quejaService: QuejaService,
    private catalogoService: CatalogoService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarQuejas();

    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cdr.detectChanges();
      },
      error: () => {
        // No es crítico: la tabla mostrará la clave en vez del nombre de la dependencia.
      },
    });
  }

  private cargarQuejas(): void {
    this.quejaService.misQuejas().subscribe({
      next: (quejas) => {
        this.quejas = quejas;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudieron cargar tus quejas.';
        this.toast.error(this.error);
        this.cdr.detectChanges();
      },
    });
  }

  // =========================================================================================
  // Presentación
  // =========================================================================================

  etiqueta(estatus: string | null | undefined): string {
    return etiquetaEstatus(estatus);
  }

  clase(estatus: string | null | undefined): string {
    return claseEstatus(estatus);
  }

  puedeEditar(queja: Queja): boolean {
    return esEditable(queja.estatus);
  }

  nombreUnidad(clave?: string): string {
    if (!clave) return '—';
    return this.dependencias.find((d) => d.clave === clave)?.nombre ?? clave;
  }

  // =========================================================================================
  // Filtrado
  // =========================================================================================

  /** Cuántas quejas hay en cada pestaña — se muestra junto al nombre. */
  get conteos(): Record<Pestania, number> {
    return {
      todas: this.quejas.length,
      tramite: this.quejas.filter((q) => !estaCerrada(q.estatus)).length,
      cerradas: this.quejas.filter((q) => estaCerrada(q.estatus)).length,
    };
  }

  get quejasFiltradas(): Queja[] {
    const texto = this.busqueda.trim().toLowerCase();

    return this.quejas.filter((q) => {
      const enPestania =
        this.pestania === 'todas' ||
        (this.pestania === 'tramite' && !estaCerrada(q.estatus)) ||
        (this.pestania === 'cerradas' && estaCerrada(q.estatus));

      if (!enPestania) return false;
      if (!texto) return true;

      // La búsqueda cubre folio, asunto y unidad académica a la vez: el usuario escribe lo
      // que recuerda de su queja sin tener que decidir en qué campo va.
      return (
        q.numeroFolio.toLowerCase().includes(texto) ||
        (q.motivo ?? '').toLowerCase().includes(texto) ||
        this.nombreUnidad(q.unidadAcademicaClave).toLowerCase().includes(texto)
      );
    });
  }

  limpiarBusqueda(): void {
    this.busqueda = '';
  }

  // =========================================================================================
  // Acciones
  // =========================================================================================

  verQueja(folio: string): void {
    this.router.navigate(['/panel/mis-quejas', folio]);
  }

  editarQueja(folio: string): void {
    this.router.navigate(['/panel/mis-quejas', folio], { queryParams: { editar: 1 } });
  }

  pedirConfirmacion(queja: Queja): void {
    if (!this.puedeEditar(queja)) {
      this.toast.advertencia(
        'Esta queja ya está en revisión y no se puede eliminar. Si necesitas retirarla, comunícate con la Defensoría.',
      );
      return;
    }
    this.folioPorCancelar = queja.numeroFolio;
  }

  cerrarConfirmacion(): void {
    if (this.cancelando) return;
    this.folioPorCancelar = null;
  }

  confirmarCancelacion(): void {
    if (!this.folioPorCancelar) return;

    this.cancelando = true;
    this.quejaService.cancelarMiQueja(this.folioPorCancelar).subscribe({
      next: (quejaActualizada) => {
        this.cancelando = false;
        this.folioPorCancelar = null;
        // Se actualiza en memoria en vez de recargar toda la lista: la respuesta ya trae la
        // queja con su estatus nuevo.
        this.quejas = this.quejas.map((q) =>
          q.numeroFolio === quejaActualizada.numeroFolio ? quejaActualizada : q,
        );
        this.toast.exito('Tu queja se retiró. El registro se conserva en la Defensoría.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cancelando = false;
        this.folioPorCancelar = null;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo retirar la queja. Intenta de nuevo.');
        this.cdr.detectChanges();
      },
    });
  }
}
