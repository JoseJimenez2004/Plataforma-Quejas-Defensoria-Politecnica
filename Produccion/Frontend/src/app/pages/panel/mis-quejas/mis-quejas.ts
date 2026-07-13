import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface FilaQueja {
  folio: string;
  fecha: string;
  asunto: string;
  unidadAcademica: string;
  estatus: string;
}

@Component({
  selector: 'app-mis-quejas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mis-quejas.html',
  styleUrl: './mis-quejas.scss',
})
export class MisQuejas {
  filtroFolio = '';
  filtroEstatus = 'Todos';

  // TODO(backend): mismo caso que Resumen — falta un GET real para listar quejas
  // por usuario en queja-service. Datos de ejemplo mientras tanto.
  quejas: FilaQueja[] = [
    { folio: 'DDP-2026-001', fecha: '2026-10-25', asunto: 'Acoso escolar', unidadAcademica: 'ESCOM', estatus: 'Recibida' },
    { folio: 'DDP-2025-098', fecha: '2025-09-15', asunto: 'Discriminación', unidadAcademica: 'ENCB', estatus: 'En Revisión' },
    { folio: 'DDP-2024-050', fecha: '2024-01-10', asunto: 'Trato indigno', unidadAcademica: 'ESCA', estatus: 'Finalizada' },
  ];

  get quejasFiltradas(): FilaQueja[] {
    return this.quejas.filter((q) => {
      const coincideFolio = !this.filtroFolio || q.folio.includes(this.filtroFolio);
      const coincideEstatus = this.filtroEstatus === 'Todos' || q.estatus === this.filtroEstatus;
      return coincideFolio && coincideEstatus;
    });
  }
}
