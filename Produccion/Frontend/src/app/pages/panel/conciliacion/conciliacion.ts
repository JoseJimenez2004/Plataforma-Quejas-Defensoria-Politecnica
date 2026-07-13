import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface AcuerdoConciliacion {
  folio: string;
  fecha: string;
  asunto: string;
  unidadAcademica: string;
}

@Component({
  selector: 'app-conciliacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conciliacion.html',
  styleUrl: './conciliacion.scss',
})
export class Conciliacion {
  // TODO(backend): no existe ningún endpoint de conciliación todavía (ni en
  // queja-service ni en un microservicio dedicado). Datos de ejemplo. Ver
  // docs/HALLAZGOS.md.
  acuerdos: AcuerdoConciliacion[] = [
    { folio: 'DDP-2026-002', fecha: '2026-10-25', asunto: 'Acoso', unidadAcademica: 'ESCOM' },
    { folio: 'DDP-2025-099', fecha: '2025-09-15', asunto: 'Discriminación', unidadAcademica: 'ESIA' },
    { folio: 'DDP-2024-002', fecha: '2024-01-10', asunto: 'Trato indigno', unidadAcademica: 'EST' },
  ];
}
