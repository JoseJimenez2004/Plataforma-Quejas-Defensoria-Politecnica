import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';

import { ResumenSimulado, generarResumenSimulado } from '../../core/utils/resumen-simulado';
import { ResumenModelo, ResumenService } from '../../core/services/resumen.service';

export interface ResumenDialogData {
  /** Folio de la queja (FOL-... o el del caso histórico). */
  folio: string;
  asunto?: string;
  /** Narrativa completa de la queja. */
  texto?: string;
}

/**
 * Resumen de una queja con el modelo (resumen-service). Mientras se genera (puede tardar hasta
 * ~2 minutos en el servidor) se muestra el progreso; si el modelo no responde, se muestra el
 * resumen SIMULADO del navegador (core/utils/resumen-simulado.ts) y la pantalla lo dice.
 */
@Component({
  selector: 'app-resumen-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './resumen-dialog.html',
  styleUrl: './resumen-dialog.css'
})
export class ResumenDialog implements OnInit, OnDestroy {
  readonly simulado: ResumenSimulado;
  modelo: ResumenModelo | null = null;
  cargando = false;
  sinModelo = false;
  private peticion?: Subscription;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ResumenDialogData,
    private resumenService: ResumenService,
    private cdr: ChangeDetectorRef
  ) {
    this.simulado = generarResumenSimulado(data.texto);
  }

  ngOnInit(): void {
    if (!this.simulado.resumen || !this.data.texto) return;

    this.cargando = true;
    this.peticion = this.resumenService.resumir(this.data.texto).subscribe({
      next: (resultado) => {
        this.modelo = resultado;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.sinModelo = true;
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Al cerrar el cuadro ya no se espera la respuesta (el servidor la guarda para la próxima vez). */
  ngOnDestroy(): void {
    this.peticion?.unsubscribe();
  }
}
