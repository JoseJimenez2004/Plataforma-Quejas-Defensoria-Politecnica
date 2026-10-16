import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Widget flotante de ayuda/información reutilizable. Antes cada formulario tenía su propia
 * caja "Nota importante" estática siempre visible; ahora es un botón fijo que abre/cierra un
 * panel flotante con el contenido que se le proyecte (`<ng-content>`), para no ocupar espacio
 * del formulario y no distraer mientras se llena.
 */
@Component({
  selector: 'app-nota-flotante',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      type="button"
      class="nota-flotante-btn"
      (click)="toggle()"
      [class.activo]="mostrar"
      aria-label="Mostrar nota importante"
    >
      ℹ
    </button>

    @if (mostrar) {
      <div class="nota-flotante-panel">
        <div class="nota-flotante-panel__header">
          <strong>{{ titulo }}</strong>
          <button type="button" class="cerrar" (click)="toggle()" aria-label="Cerrar">✕</button>
        </div>
        <div class="nota-flotante-panel__contenido">
          <ng-content></ng-content>
        </div>
      </div>
    }
  `,
  styleUrl: './nota-flotante.scss',
})
export class NotaFlotante {
  @Input() titulo = 'Nota importante';

  mostrar = false;

  toggle(): void {
    this.mostrar = !this.mostrar;
  }
}
