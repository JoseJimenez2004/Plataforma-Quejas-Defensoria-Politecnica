import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ConciliacionService } from '../../../core/services/conciliacion.service';
import { ConciliacionDTO } from '../../../models/perfil.models';

@Component({
  selector: 'app-conciliacion',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, LucideAngularModule],
  templateUrl: './conciliacion.component.html',
})
export class ConciliacionComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private conciliacionService = inject(ConciliacionService);
  private cdr = inject(ChangeDetectorRef);

  conciliacion: ConciliacionDTO | null = null;
  cargando = true;
  error = '';
  mostrarRechazo = false;
  justificacion = '';

  ngOnInit(): void {
    const folio = this.route.snapshot.paramMap.get('folio')!;
    this.conciliacionService.obtener(folio).subscribe({
      next: (c) => {
        this.conciliacion = c;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la propuesta de conciliación.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  aceptar(): void {
    // TODO: llamar endpoint de aceptación cuando exista
    alert('Propuesta aceptada. Se procederá con los términos del acuerdo.');
  }

  rechazar(): void {
    this.mostrarRechazo = true;
  }

  confirmarRechazo(): void {
    if (!this.justificacion.trim()) return;
    // TODO: llamar endpoint de rechazo cuando exista
    alert('Propuesta rechazada. El defensor analizará su caso nuevamente.');
  }
}
