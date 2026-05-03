import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { QuejaService } from '../../../core/services/queja.service';
import { QuejaSeguimientoDTO, EvidenciaDTO, EstatusQueja } from '../../../models/queja.models';

@Component({
  selector: 'app-edicion-queja',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './edicion-queja.component.html',
})
export class EdicionQuejaComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private quejaService = inject(QuejaService);
  private cdr = inject(ChangeDetectorRef);

  folio = '';
  queja: QuejaSeguimientoDTO | null = null;
  cargando = true;
  guardando = false;
  error = '';

  evidenciasAEliminar: number[] = [];
  nuevosArchivos: File[] = [];

  form = this.fb.group({
    descripcionHechos: ['', [Validators.required, Validators.minLength(30)]],
  });

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio')!;
    this.quejaService.seguimientoPrivado(this.folio).subscribe({
      next: (q) => {
        if (q.estatusActual !== EstatusQueja.RECIBIDA) {
          this.router.navigate(['/dashboard/queja', this.folio]);
          return;
        }
        this.queja = q;
        this.form.patchValue({ descripcionHechos: q.descripcionHechos });
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la queja.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  marcarEvidenciaParaBorrar(id: number): void {
    if (!this.evidenciasAEliminar.includes(id)) {
      this.evidenciasAEliminar.push(id);
    }
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.nuevosArchivos = [...this.nuevosArchivos, ...Array.from(input.files)];
      input.value = '';
    }
  }

  eliminarNuevoArchivo(i: number): void {
    this.nuevosArchivos.splice(i, 1);
  }

  get evidenciasVisibles(): EvidenciaDTO[] {
    return this.queja?.evidencias.filter((e) => !this.evidenciasAEliminar.includes(e.id)) ?? [];
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando = true;
    this.error = '';
    const { descripcionHechos } = this.form.value;

    this.quejaService
      .editar(this.folio, { asunto: "", descripcionHechos: descripcionHechos!, evidenciasBorrarIds: this.evidenciasAEliminar }, this.nuevosArchivos)
      .subscribe({
        next: () => this.router.navigate(['/dashboard/queja', this.folio]),
        error: (err) => {
          this.error = err.error?.mensaje ?? 'Error al guardar los cambios.';
          this.guardando = false;
          this.cdr.detectChanges();
        },
      });
  }

  cancelar(): void {
    this.router.navigate(['/dashboard/queja', this.folio]);
  }
}
