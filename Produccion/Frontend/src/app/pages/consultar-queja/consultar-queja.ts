import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QuejaService } from '../../core/services/queja.service';

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

  constructor(
    private quejaService: QuejaService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.folio = params.get('folio') ?? '';
    this.correo = params.get('correo') ?? '';
    if (this.folio && this.correo) {
      this.consultar();
    }
  }

  consultar(): void {
    if (!this.folio || !this.correo) {
      this.error = 'Ingresa folio y correo.';
      return;
    }
    this.error = '';
    this.cargando = true;
    this.resultado = null;

    this.quejaService.validarFolio({ folio: this.folio, correo: this.correo }).subscribe({
      next: (existe) => {
        this.resultado = existe ? 'encontrada' : 'no-encontrada';
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudo consultar en este momento. Intenta más tarde.';
        this.cargando = false;
      },
    });
  }
}
