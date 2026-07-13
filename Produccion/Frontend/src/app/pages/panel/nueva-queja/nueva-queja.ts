import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { QuejaService } from '../../../core/services/queja.service';
import { Queja } from '../../../core/models/queja.models';

@Component({
  selector: 'app-nueva-queja',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './nueva-queja.html',
  styleUrl: './nueva-queja.scss',
})
export class NuevaQueja {
  unidadAcademica = '';
  fechaHechos = '';
  nombreDenunciado = '';
  apellidoDenunciado = '';
  relato = '';
  archivo: File | null = null;

  cargando = false;
  error = '';
  quejaCreada: Queja | null = null;

  constructor(
    private quejaService: QuejaService,
    private router: Router,
  ) {}

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo = input.files?.[0] ?? null;
  }

  enviar(): void {
    this.error = '';

    if (!this.unidadAcademica || !this.relato) {
      this.error = 'Completa al menos la unidad académica y el relato de los hechos.';
      return;
    }

    // El backend de queja-service solo guarda "motivo" y "descripcion" — combinamos
    // el resto de los campos del wireframe (lugar, fecha, denunciado) dentro de la
    // descripción hasta que el modelo Queja del backend los soporte como campos propios.
    const motivo = `Queja en ${this.unidadAcademica}`;
    const descripcionCompleta = [
      `Unidad académica: ${this.unidadAcademica}`,
      this.fechaHechos ? `Fecha de los hechos: ${this.fechaHechos}` : '',
      this.nombreDenunciado || this.apellidoDenunciado
        ? `Denunciado: ${this.nombreDenunciado} ${this.apellidoDenunciado}`
        : '',
      '',
      'Relato:',
      this.relato,
    ]
      .filter(Boolean)
      .join('\n');

    this.cargando = true;
    this.quejaService.registrarQueja(motivo, descripcionCompleta, this.archivo).subscribe({
      next: (queja) => {
        this.cargando = false;
        this.quejaCreada = queja;
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.mensaje ?? 'No se pudo registrar la queja. Intenta de nuevo.';
      },
    });
  }

  volverAlTablero(): void {
    this.router.navigate(['/panel']);
  }
}
