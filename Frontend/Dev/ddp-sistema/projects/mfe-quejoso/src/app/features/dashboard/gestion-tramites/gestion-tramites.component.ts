import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { TramitesService } from '../../../core/services/tramites.service';
import { QuejaService } from '../../../core/services/queja.service';
import { QuejaSeguimientoDTO, EstatusQueja, ESTATUS_LABEL } from '../../../models/queja.models';
import { TramitesResumenDTO } from '../../../models/perfil.models';

@Component({
  selector: 'app-gestion-tramites',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './gestion-tramites.component.html',
})
export class GestionTramitesComponent implements OnInit {
  private tramitesService = inject(TramitesService);
  private quejaService = inject(QuejaService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  resumen: TramitesResumenDTO = { totales: 0, enProceso: 0, finalizadas: 0, accionesPendientes: 0 };
  quejas: QuejaSeguimientoDTO[] = [];
  cargando = true;
  busquedaFolio = '';

  mostrarModalEliminar = false;
  folioAEliminar = '';
  eliminando = false;

  readonly ESTATUS_LABEL = ESTATUS_LABEL;
  readonly EstatusQueja = EstatusQueja;

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.tramitesService.resumen().subscribe({
      next: (r) => {
        this.resumen = r;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
    this.tramitesService.historial({}).subscribe({
      next: (q) => {
        this.quejas = q;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  get quejasFiltradas(): QuejaSeguimientoDTO[] {
    if (!this.busquedaFolio.trim()) return this.quejas;
    const b = this.busquedaFolio.toUpperCase();
    return this.quejas.filter((q) => q.folio.toUpperCase().includes(b));
  }

  puedeEditar(q: QuejaSeguimientoDTO): boolean {
    return q.estatusActual === EstatusQueja.RECIBIDA;
  }

  verDetalle(folio: string): void {
    this.router.navigate(['/dashboard/queja', folio]);
  }

  editarQueja(folio: string): void {
    this.router.navigate(['/dashboard/queja', folio, 'editar']);
  }

  abrirModalEliminar(folio: string): void {
    this.folioAEliminar = folio;
    this.mostrarModalEliminar = true;
  }

  cancelarEliminar(): void {
    this.mostrarModalEliminar = false;
    this.folioAEliminar = '';
  }

  confirmarEliminar(): void {
    if (!this.folioAEliminar) return;
    this.eliminando = true;
    this.quejaService.borrar(this.folioAEliminar).subscribe({
      next: () => {
        this.mostrarModalEliminar = false;
        this.eliminando = false;
        this.cdr.detectChanges();
        this.cargarDatos();
      },
      error: () => {
        this.eliminando = false;
        this.cdr.detectChanges();
      },
    });
  }

  etiquetaEstatus(estatus: EstatusQueja): string {
    return ESTATUS_LABEL[estatus] ?? estatus;
  }
}
