import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../header/header.component';
import { FooterComponent } from '../footer/footer.component';
import { QuejaService } from '../../core/services/queja.service';
import { QuejaSeguimientoDTO, EstatusQueja, ESTATUS_LABEL } from '../../models/queja.models';

interface PasoSeguimiento {
  label: string;
  fecha?: string;
  completado: boolean;
  activo: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule, HeaderComponent, FooterComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private quejaService = inject(QuejaService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  correoSeguimiento = '';
  folioSeguimiento = '';
  buscando = false;
  errorSeguimiento = '';
  resultadoSeguimiento: QuejaSeguimientoDTO | null = null;
  mostrarModal = false;
  currentYear = new Date().getFullYear();

  consultar(): void {
    if (!this.correoSeguimiento.trim() || !this.folioSeguimiento.trim()) {
      this.errorSeguimiento = 'Ingresa el correo y el folio.';
      return;
    }
    this.buscando = true;
    this.errorSeguimiento = '';
    this.quejaService.seguimientoPublico(this.folioSeguimiento, this.correoSeguimiento).subscribe({
      next: (q) => {
        this.resultadoSeguimiento = q;
        this.mostrarModal = true;
        this.buscando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorSeguimiento = 'No se encontró la queja. Verifica el folio y correo.';
        this.buscando = false;
        this.cdr.detectChanges();
      },
    });
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.resultadoSeguimiento = null;
  }

  get pasos(): PasoSeguimiento[] {
    if (!this.resultadoSeguimiento) return [];
    const e = this.resultadoSeguimiento.estatusActual;
    const fecha = this.resultadoSeguimiento.fechaRegistro;

    const enRevision = [
      EstatusQueja.EN_ANALISIS, EstatusQueja.COMPETENTE, EstatusQueja.ASIGNADO_A_ABOGADO,
      EstatusQueja.EN_INVESTIGACION, EstatusQueja.PROCESO_CONCLUSION, EstatusQueja.FINALIZADA,
    ].includes(e);
    const asignado = [
      EstatusQueja.ASIGNADO_A_ABOGADO, EstatusQueja.EN_INVESTIGACION,
      EstatusQueja.PROCESO_CONCLUSION, EstatusQueja.FINALIZADA,
    ].includes(e);
    const finalizado = e === EstatusQueja.FINALIZADA;

    return [
      { label: 'Queja Recibida', fecha, completado: true, activo: false },
      { label: 'En Revisión', fecha: enRevision ? fecha : undefined, completado: asignado || finalizado, activo: enRevision && !asignado },
      { label: 'Asignación de Defensor', completado: finalizado, activo: asignado && !finalizado },
      { label: 'Resolución Final', completado: finalizado, activo: e === EstatusQueja.PROCESO_CONCLUSION },
    ];
  }

  get badgeClass(): string {
    const e = this.resultadoSeguimiento?.estatusActual;
    if (e === EstatusQueja.FINALIZADA) return 'bg-green-600 text-white';
    if (e === EstatusQueja.CANCELADA || e === EstatusQueja.IMPROCEDENTE || e === EstatusQueja.REMISION)
      return 'bg-gray-500 text-white';
    if (e === EstatusQueja.RECIBIDA) return 'bg-gray-600 text-white';
    return 'bg-yellow-500 text-white';
  }

  etiquetaEstatus(e: EstatusQueja): string {
    return ESTATUS_LABEL[e] ?? e;
  }
}
