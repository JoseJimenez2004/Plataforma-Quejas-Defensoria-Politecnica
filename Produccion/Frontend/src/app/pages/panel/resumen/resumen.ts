import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

interface FilaResumen {
  folio: string;
  fecha: string;
  asunto: string;
  estatus: string;
}

@Component({
  selector: 'app-resumen',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './resumen.html',
  styleUrl: './resumen.scss',
})
export class Resumen {
  // TODO(backend): queja-service no expone todavía un endpoint para listar las quejas
  // de un usuario (solo /registrar y /validar-folio). Estos datos son de ejemplo
  // hasta que exista un GET /api/quejoso/quejas?correo=... real. Ver docs/HALLAZGOS.md.
  quejasEjemplo: FilaResumen[] = [
    { folio: 'DDP-2026-001', fecha: '2026-10-25', asunto: 'Acoso escolar', estatus: 'Recibida' },
    {
      folio: 'DDP-2026-002',
      fecha: '2026-10-25',
      asunto: 'Trato discriminatorio en unidad académica',
      estatus: 'En Revisión',
    },
    { folio: 'DDP-2026-003', fecha: '2026-10-24', asunto: 'Conflicto laboral', estatus: 'Finalizada' },
  ];

  get totales(): number {
    return this.quejasEjemplo.length;
  }

  get enProceso(): number {
    return this.quejasEjemplo.filter((q) => q.estatus !== 'Finalizada').length;
  }

  get finalizadas(): number {
    return this.quejasEjemplo.filter((q) => q.estatus === 'Finalizada').length;
  }
}
