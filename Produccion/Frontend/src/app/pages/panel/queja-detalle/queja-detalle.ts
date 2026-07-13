import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-queja-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './queja-detalle.html',
  styleUrl: './queja-detalle.scss',
})
export class QuejaDetalle implements OnInit {
  folio = '';
  modoEdicion = false;

  // TODO(backend): no existe todavía un GET /api/quejoso/quejas/{folio} — esta vista
  // usa datos de ejemplo. Ver docs/HALLAZGOS.md.
  fechaRegistro = '2026-10-25';
  asunto = 'Trato discriminatorio en unidad académica';
  descripcion = 'El relato original de la queja.';
  estatus: 'Recibida' | 'En Revisión' | 'Finalizada' = 'En Revisión';
  evidencias = ['documento_prueba.pdf', 'imagen_hechos.jpg'];

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
  }

  get puedeEditar(): boolean {
    return this.estatus === 'Recibida';
  }

  guardarCambios(): void {
    this.modoEdicion = false;
  }
}
