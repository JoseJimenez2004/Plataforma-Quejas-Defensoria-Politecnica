import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AVISO_PRIVACIDAD_VERSION } from '../../core/validaciones/reglas-queja';

/**
 * Ventana emergente con el aviso de privacidad, obligatoria antes de capturar una queja.
 *
 * El usuario tiene que DESLIZAR el texto hasta el final para que se habilite la casilla
 * "He leído y acepto"; al marcarla, el modal se cierra y se puede continuar con el
 * formulario. Mientras no acepte, el modal bloquea la pantalla (no hay botón de cerrar) —
 * o sale del formulario con "No acepto".
 *
 * Detalle importante: si el texto cabe completo sin necesidad de scroll (pantalla grande,
 * zoom reducido), no habría evento de scroll nunca y la casilla quedaría deshabilitada para
 * siempre. Por eso, al terminar de renderizar se comprueba si hay algo que deslizar y, si no
 * lo hay, se habilita de una vez.
 */
@Component({
  selector: 'app-aviso-privacidad',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './aviso-privacidad.html',
  styleUrl: './aviso-privacidad.scss',
})
export class AvisoPrivacidad implements AfterViewInit {
  /** Se emite con la versión aceptada cuando el usuario marca la casilla. */
  @Output() aceptado = new EventEmitter<string>();

  @ViewChild('cuerpoAviso') cuerpoAviso?: ElementRef<HTMLDivElement>;

  /** Se habilita cuando el texto se leyó hasta abajo. */
  llegoAlFinal = false;
  aceptaCasilla = false;

  readonly version = AVISO_PRIVACIDAD_VERSION;

  /** Margen en píxeles para dar por leído el texto sin exigir el píxel exacto. */
  private readonly MARGEN_FINAL = 12;

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngAfterViewInit(): void {
    // Si no hay nada que deslizar, se da por leído: de lo contrario la casilla nunca
    // se habilitaría y el formulario quedaría bloqueado.
    setTimeout(() => {
      const elemento = this.cuerpoAviso?.nativeElement;
      if (elemento && elemento.scrollHeight <= elemento.clientHeight + this.MARGEN_FINAL) {
        this.llegoAlFinal = true;
        this.cdr.detectChanges();
      }
    });
  }

  onScroll(event: Event): void {
    const elemento = event.target as HTMLElement;
    const alFinal =
      elemento.scrollTop + elemento.clientHeight >= elemento.scrollHeight - this.MARGEN_FINAL;
    if (alFinal && !this.llegoAlFinal) {
      this.llegoAlFinal = true;
      this.cdr.detectChanges();
    }
  }

  /** Marcar la casilla cierra el modal, que es justo lo que se pidió. */
  onAceptar(marcada: boolean): void {
    this.aceptaCasilla = marcada;
    if (marcada) {
      this.aceptado.emit(this.version);
    }
  }

  /** El consentimiento tiene que poder negarse: se sale del formulario. */
  onNoAceptar(): void {
    this.router.navigate(['/inicio']);
  }

  irAlFinal(): void {
    const elemento = this.cuerpoAviso?.nativeElement;
    elemento?.scrollTo({ top: elemento.scrollHeight, behavior: 'smooth' });
  }
}
