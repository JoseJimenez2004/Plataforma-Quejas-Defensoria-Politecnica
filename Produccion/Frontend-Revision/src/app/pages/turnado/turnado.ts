import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { AntecedenteItem, CargaAbogado, QuejaDetalle } from '../../core/models/revision.models';

@Component({
  selector: 'app-turnado',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './turnado.html',
  styleUrl: './turnado.scss',
})
export class Turnado implements OnInit {
  folio = '';
  queja: QuejaDetalle | null = null;
  antecedentes: AntecedenteItem[] = [];
  abogados: CargaAbogado[] = [];
  cargando = true;
  turnando = false;

  defensorAsignado = '';
  comentarios = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private revisionService: RevisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    this.cargar();
  }

  private cargar(): void {
    this.cargando = true;
    forkJoin({
      queja: this.revisionService.detalle(this.folio),
      antecedentes: this.revisionService.antecedentes(this.folio),
      abogados: this.revisionService.abogados(this.folio),
    }).subscribe({
      next: ({ queja, antecedentes, abogados }) => {
        this.queja = queja;
        this.antecedentes = antecedentes;
        this.abogados = abogados;
        // Se preselecciona al abogado sugerido; la recepcionista puede cambiarlo.
        this.defensorAsignado = abogados.find((a) => a.sugerido)?.nombre ?? '';
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la información de turnado.');
        this.cdr.detectChanges();
      },
    });
  }

  elegir(abogado: CargaAbogado): void {
    this.defensorAsignado = abogado.nombre;
  }

  /** Ancho de la barra de carga relativo a quien más quejas en trámite lleva. */
  anchoCarga(abogado: CargaAbogado): number {
    const maximo = Math.max(1, ...this.abogados.map((a) => a.cargaActiva));
    return Math.round((abogado.cargaActiva / maximo) * 100);
  }

  /** Aviso cuando se elige a alguien con más carga que el sugerido (sin bloquear el turnado). */
  get avisoCarga(): string {
    const elegido = this.abogados.find((a) => a.nombre === this.defensorAsignado);
    const sugerido = this.abogados.find((a) => a.sugerido);
    if (!elegido || !sugerido || elegido === sugerido || elegido.cargaActiva <= sugerido.cargaActiva) {
      return '';
    }
    return `${elegido.nombre} lleva ${elegido.cargaActiva} quejas en trámite; ${sugerido.nombre} lleva ${sugerido.cargaActiva}. `
      + 'Confirma que hay una razón para asignársela.';
  }

  pausar(): void {
    this.toast.info('Avance guardado. La queja sigue "En Proceso".');
    this.router.navigate(['/']);
  }

  turnar(): void {
    if (!this.defensorAsignado) {
      this.toast.advertencia('Selecciona el defensor responsable.');
      return;
    }

    this.turnando = true;
    this.revisionService.turnar(this.folio, {
      defensorAsignado: this.defensorAsignado,
      comentarios: this.comentarios,
    }).subscribe({
      next: () => {
        this.turnando = false;
        this.toast.exito('Queja turnada correctamente.');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.turnando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo turnar la queja.');
        this.cdr.detectChanges();
      },
    });
  }
}
