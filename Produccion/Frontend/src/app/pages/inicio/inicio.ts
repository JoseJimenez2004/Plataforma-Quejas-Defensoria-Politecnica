import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

interface TabContenido {
  id: string;
  titulo: string;
  fecha: string;
}

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  correo = '';
  folio = '';
  errorConsulta = '';

  readonly tabs = ['Difusión', 'Institucional', 'Publicaciones', 'Investigación', 'Servicios'];
  readonly tabActiva = signal(this.tabs[0]);

  private readonly contenidoPorTab: Record<string, TabContenido[]> = {
    Difusión: [
      { id: 'd1', titulo: 'Semana de la orientación y derechos estudiantiles', fecha: '10, jul 2026' },
      { id: 'd2', titulo: 'Jornada de difusión de la Defensoría en unidades foráneas', fecha: '25, jun 2026' },
      { id: 'd3', titulo: 'Campaña "Conoce tus derechos politécnicos"', fecha: '16, jun 2026' },
      { id: 'd4', titulo: 'Convocatoria: taller de mediación estudiantil', fecha: '12, jun 2026' },
    ],
    Institucional: [
      { id: 'i1', titulo: 'Acuerdo de creación de la Defensoría de los Derechos Politécnicos', fecha: '—' },
      { id: 'i2', titulo: 'Organigrama y funciones de la Defensoría', fecha: '—' },
      { id: 'i3', titulo: 'Informe anual de actividades', fecha: '—' },
    ],
    Publicaciones: [
      { id: 'p1', titulo: 'Guía práctica: cómo presentar una queja', fecha: '—' },
      { id: 'p2', titulo: 'Boletín trimestral de la Defensoría', fecha: '—' },
    ],
    Investigación: [
      { id: 'iv1', titulo: 'Diagnóstico sobre derechos estudiantiles en el IPN', fecha: '—' },
      { id: 'iv2', titulo: 'Estudio sobre mecanismos de conciliación escolar', fecha: '—' },
    ],
    Servicios: [
      { id: 's1', titulo: 'Orientación jurídica y psicológica', fecha: '—' },
      { id: 's2', titulo: 'Acompañamiento en procesos de queja', fecha: '—' },
      { id: 's3', titulo: 'Conciliación entre las partes', fecha: '—' },
    ],
  };

  constructor(private router: Router) {}

  get contenidoActivo(): TabContenido[] {
    return this.contenidoPorTab[this.tabActiva()] ?? [];
  }

  seleccionarTab(tab: string): void {
    this.tabActiva.set(tab);
  }

  consultar(): void {
    this.errorConsulta = '';

    if (!this.correo.trim() || !this.folio.trim()) {
      this.errorConsulta = 'Ingresa tu correo y tu número de folio para poder consultar tu queja.';
      return;
    }

    const correoValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.correo.trim());
    if (!correoValido) {
      this.errorConsulta = 'El correo no tiene un formato válido (ejemplo: nombre@dominio.com).';
      return;
    }

    this.router.navigate(['/queja/consultar'], {
      queryParams: { folio: this.folio.trim(), correo: this.correo.trim() },
    });
  }
}
