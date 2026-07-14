import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QuejaService } from '../../core/services/queja.service';
import { Queja, etiquetaEstatus } from '../../core/models/queja.models';

@Component({
  selector: 'app-consultar-queja',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultar-queja.html',
  styleUrl: './consultar-queja.scss',
})
export class ConsultarQueja implements OnInit {
  folio = '';
  correo = '';
  cargando = false;
  resultado: 'encontrada' | 'no-encontrada' | null = null;
  error = '';
  queja: Queja | null = null;

  constructor(
    private quejaService: QuejaService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.folio = params.get('folio') ?? '';
    this.correo = params.get('correo') ?? '';
    if (this.folio && this.correo) {
      this.consultar();
    }
  }

  get estatus(): string {
    return etiquetaEstatus(this.queja?.estatus);
  }

  consultar(): void {
    if (!this.folio || !this.correo) {
      this.error = 'Ingresa folio y correo.';
      return;
    }
    this.error = '';
    this.cargando = true;
    this.resultado = null;
    this.queja = null;

    // Usamos el endpoint público que regresa el detalle real de la queja (no solo un
    // true/false) para mostrar folio, estatus, motivo y fecha directamente en esta pantalla.
    this.quejaService.obtenerPorFolio(this.folio, this.correo).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.resultado = 'encontrada';
        this.cargando = false;
        // App zoneless (sin zone.js): sin esto la respuesta llega pero la vista se queda
        // pegada en "Consultando…" aunque los datos ya estén disponibles.
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        // El backend responde 400 (no 404) cuando el folio+correo no coinciden con ninguna
        // queja — ver GlobalExceptionHandler de queja-service.
        if (err?.status === 400) {
          this.resultado = 'no-encontrada';
        } else {
          this.error = 'No se pudo consultar en este momento. Intenta más tarde.';
        }
        this.cdr.detectChanges();
      },
    });
  }
}
