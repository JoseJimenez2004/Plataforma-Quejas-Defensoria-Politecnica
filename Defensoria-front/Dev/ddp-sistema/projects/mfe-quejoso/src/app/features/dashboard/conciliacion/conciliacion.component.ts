import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
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
  private router = inject(Router);
  private conciliacionService = inject(ConciliacionService);
  private cdr = inject(ChangeDetectorRef);
  private sanitizer = inject(DomSanitizer);

  conciliacion: ConciliacionDTO | null = null;
  pdfSeguro: SafeResourceUrl | null = null;
  cargando = true;
  error = '';
  mostrarRechazo = false;
  justificacion = '';
  procesando = false;
  exito = '';
  errorAccion = '';

  ngOnInit(): void {
    const folio = this.route.snapshot.paramMap.get('folio')!;
    this.conciliacionService.obtener(folio).subscribe({
      next: (c) => {
        this.conciliacion = c;
        if (c.urlPdf) {
          this.pdfSeguro = this.sanitizer.bypassSecurityTrustResourceUrl(c.urlPdf);
        }
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
    if (!this.conciliacion) return;
    this.procesando = true;
    this.errorAccion = '';
    this.conciliacionService.aceptar(this.conciliacion.folioQueja).subscribe({
      next: () => {
        this.exito = 'Acuerdo aceptado. El defensor será notificado para proceder.';
        this.procesando = false;
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/dashboard/acuerdos']), 1800);
      },
      error: (err) => {
        this.errorAccion = err.error?.mensaje ?? 'No se pudo registrar la aceptación.';
        this.procesando = false;
        this.cdr.detectChanges();
      },
    });
  }

  rechazar(): void {
    this.mostrarRechazo = true;
    this.errorAccion = '';
  }

  confirmarRechazo(): void {
    if (!this.conciliacion || !this.justificacion.trim()) return;
    this.procesando = true;
    this.errorAccion = '';
    this.conciliacionService.rechazar(this.conciliacion.folioQueja, this.justificacion.trim()).subscribe({
      next: () => {
        this.exito = 'Rechazo registrado. El defensor analizará tu justificación.';
        this.procesando = false;
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/dashboard/acuerdos']), 1800);
      },
      error: (err) => {
        this.errorAccion = err.error?.mensaje ?? 'No se pudo registrar el rechazo.';
        this.procesando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
