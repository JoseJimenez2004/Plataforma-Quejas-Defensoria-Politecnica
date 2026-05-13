import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ConciliacionService } from '../../../core/services/conciliacion.service';
import { AcuerdoPendienteDTO } from '../../../models/perfil.models';

@Component({
  selector: 'app-acuerdos',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './acuerdos.component.html',
})
export class AcuerdosComponent implements OnInit {
  private conciliacionService = inject(ConciliacionService);
  private cdr = inject(ChangeDetectorRef);

  acuerdos: AcuerdoPendienteDTO[] = [];
  cargando = true;
  error = '';

  ngOnInit(): void {
    this.conciliacionService.listarPendientes().subscribe({
      next: (data) => {
        this.acuerdos = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los acuerdos pendientes.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
