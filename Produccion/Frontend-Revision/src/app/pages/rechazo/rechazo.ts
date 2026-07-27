import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { QuejaDetalle } from '../../core/models/revision.models';

interface MotivoOpcion {
  texto: string;
  marcado: boolean;
}

@Component({
  selector: 'app-rechazo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rechazo.html',
  styleUrl: './rechazo.scss',
})
export class Rechazo implements OnInit {
  folio = '';
  queja: QuejaDetalle | null = null;
  cargando = true;
  enviando = false;

  observaciones = '';

  motivos: MotivoOpcion[] = [
    { texto: 'Documentación ilegible o incompleta.', marcado: false },
    { texto: 'Falta de pruebas mínimas requeridas.', marcado: false },
    { texto: 'El asunto no es competencia de esta Defensoría.', marcado: false },
    { texto: 'Datos de contacto erróneos.', marcado: false },
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

  regresar(): void {
    this.router.navigate(['/validacion', this.folio]);
  }

  confirmarRechazo(): void {
    const motivosMarcados = this.motivos.filter((m) => m.marcado).map((m) => m.texto);

    if (motivosMarcados.length === 0 && !this.observaciones.trim()) {
      this.toast.advertencia('Selecciona al menos un motivo o escribe una observación.');
      return;
    }

    this.enviando = true;
    this.revisionService.rechazar(this.folio, {
      motivos: motivosMarcados,
      observaciones: this.observaciones,
    }).subscribe({
      next: () => {
        this.enviando = false;
        this.toast.exito('Rechazo enviado. Se notificó al quejoso por correo.');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.enviando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo enviar el rechazo.');
        this.cdr.detectChanges();
      },
    });
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
