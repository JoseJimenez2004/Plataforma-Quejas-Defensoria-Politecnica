import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { QuejaDetalle } from '../../core/models/revision.models';

interface RequisitoChecklist {
  clave: string;
  etiqueta: string;
  cumplido: boolean;
}

@Component({
  selector: 'app-validacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './validacion.html',
  styleUrl: './validacion.scss',
})
export class Validacion implements OnInit {
  folio = '';
  queja: QuejaDetalle | null = null;
  cargando = true;

  requisitos: RequisitoChecklist[] = [
    { clave: 'identificacion', etiqueta: 'Identificación Oficial: Vigente.', cumplido: false },
    { clave: 'datos', etiqueta: 'Datos Completos: Nombre, boleta/empleado y contacto correctos.', cumplido: false },
    { clave: 'relato', etiqueta: 'Relato de Hechos: claro y suficiente.', cumplido: false },
    { clave: 'evidencias', etiqueta: 'Evidencias Mínimas: legibles y relacionadas.', cumplido: false },
  ];

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

  get todosLosRequisitosCumplidos(): boolean {
    return this.requisitos.every((r) => r.cumplido);
  }

  verDocumento(id: number): void {
    this.revisionService.descargarEvidencia(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => this.toast.error('No se pudo abrir el documento.'),
    });
  }

  irACanalizar(): void {
    if (!this.todosLosRequisitosCumplidos) {
      this.toast.advertencia('Marca los 4 requisitos antes de canalizar la queja.');
      return;
    }
    this.router.navigate(['/turnado', this.folio]);
  }

  irARechazar(): void {
    this.router.navigate(['/rechazo', this.folio]);
  }

  private cargar(): void {
    this.cargando = true;
    this.revisionService.detalle(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la queja.');
        this.cdr.detectChanges();
      },
    });
  }
}
