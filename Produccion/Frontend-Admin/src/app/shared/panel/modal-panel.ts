import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

/** Ventana emergente del panel: título guinda, subtítulo, botón de cerrar y contenido libre. */
@Component({
  selector: 'app-modal-panel',
  standalone: true,
  template: `
    <div class="seg-modal-fondo" (click)="cerrar.emit()">
      <section class="seg-modal" [class.ancho]="ancho" role="dialog" aria-modal="true" [attr.aria-label]="titulo"
               (click)="$event.stopPropagation()">
        <header class="seg-modal-cabeza">
          <div>
            <h2>{{ titulo }}</h2>
            @if (subtitulo) { <p>{{ subtitulo }}</p> }
          </div>
          <div class="seg-modal-acciones">
            <ng-content select="[acciones]"></ng-content>
            <button type="button" class="seg-modal-cerrar" (click)="cerrar.emit()" aria-label="Cerrar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>
        </header>
        <div class="seg-modal-cuerpo"><ng-content></ng-content></div>
      </section>
    </div>
  `,
})
export class ModalPanel {
  @Input() titulo = '';
  @Input() subtitulo = '';
  @Input() ancho = true;
  @Output() cerrar = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  alEscape(): void {
    this.cerrar.emit();
  }
}
