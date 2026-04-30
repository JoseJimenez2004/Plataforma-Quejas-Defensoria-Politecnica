import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { TramitesService } from '../../../core/services/tramites.service';
import { QuejaSeguimientoDTO, EstatusQueja, ESTATUS_LABEL, QuejaFiltroDTO } from '../../../models/queja.models';

@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './historial.component.html',
})
export class HistorialComponent implements OnInit {
  private tramitesService = inject(TramitesService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  quejas: QuejaSeguimientoDTO[] = [];
  cargando = false;
  readonly ESTATUS_LABEL = ESTATUS_LABEL;
  readonly EstatusQueja = EstatusQueja;
  readonly estatusOpciones = Object.values(EstatusQueja);

  filtro: QuejaFiltroDTO = { folio: '', estatus: '', fechaInicio: '', fechaFin: '' };

  ngOnInit(): void {
    this.buscar();
  }

  buscar(): void {
    this.cargando = true;
    this.tramitesService.historial(this.filtro).subscribe({
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

  limpiar(): void {
    this.filtro = { folio: '', estatus: '', fechaInicio: '', fechaFin: '' };
    this.buscar();
  }

  verDetalle(folio: string): void {
    this.router.navigate(['/dashboard/queja', folio]);
  }

  etiquetaEstatus(e: EstatusQueja): string {
    return ESTATUS_LABEL[e] ?? e;
  }
}
