import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Aviso {
  texto: string;
  hace: string;
  accion?: string;
}

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.scss',
})
export class Notificaciones {
  // TODO(backend): notificaciones-service solo expone POST /enviar (correo saliente),
  // no hay un "centro de notificaciones" por usuario que el frontend pueda consultar.
  // Datos de ejemplo. Ver docs/HALLAZGOS.md.
  avisos: Aviso[] = [
    {
      texto: 'Nueva Propuesta de Conciliación: se generó una propuesta para tu queja DDP-2026-002.',
      hace: 'Hace 20 min',
      accion: 'Ver Propuesta',
    },
    {
      texto: 'Estatus Actualizado: tu queja DDP-2026-001 ha pasado a la etapa de "En Revisión".',
      hace: 'Ayer, 15:40',
    },
    {
      texto: 'Cuenta Activada: bienvenido al sistema de seguimiento de la Defensoría.',
      hace: '07/10/2026',
    },
  ];
}
