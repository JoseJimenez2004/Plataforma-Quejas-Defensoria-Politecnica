import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { AntecedenteItem, DefensorOpcion, QuejaDetalle } from '../../core/models/revision.models';

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
  defensores: DefensorOpcion[] = [];
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
      defensores: this.revisionService.defensores(),
    }).subscribe({
      next: ({ queja, antecedentes, defensores }) => {
        this.queja = queja;
        this.antecedentes = antecedentes;
        this.defensores = defensores;
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
