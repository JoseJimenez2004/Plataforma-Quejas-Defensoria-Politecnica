import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { BandejaResumen } from '../../core/models/revision.models';

@Component({
  selector: 'app-bandeja',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.scss',
})
export class Bandeja implements OnInit {
  resumen: BandejaResumen | null = null;
  cargando = true;

  constructor(
    private revisionService: RevisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando = true;
    this.revisionService.bandeja().subscribe({
      next: (resumen) => {
        this.resumen = resumen;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar la bandeja de entrada.');
        this.cdr.detectChanges();
      },
    });
  }
}
