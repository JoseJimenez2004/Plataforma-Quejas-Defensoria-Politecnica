import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { QuejaService } from '../../../core/services/queja.service';
import { QuejaSeguimientoDTO, EvidenciaDTO, EstatusQueja, ESTATUS_LABEL } from '../../../models/queja.models';

interface PasoTramite {
  label: string;
  fecha?: string;
  completado: boolean;
  activo: boolean;
}

@Component({
  selector: 'app-detalle-queja',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './detalle-queja.component.html',
})
export class DetalleQuejaComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private quejaService = inject(QuejaService);
  private cdr = inject(ChangeDetectorRef);

  queja: QuejaSeguimientoDTO | null = null;
  cargando = true;
  error = '';
  readonly ESTATUS_LABEL = ESTATUS_LABEL;
  readonly EstatusQueja = EstatusQueja;

  get pasos(): PasoTramite[] {
    if (!this.queja) return [];
    const e = this.queja.estatusActual;
    const completados: EstatusQueja[] = [
      EstatusQueja.EN_ANALISIS, EstatusQueja.ASIGNADO_A_ABOGADO,
      EstatusQueja.EN_INVESTIGACION, EstatusQueja.PROCESO_CONCLUSION, EstatusQueja.FINALIZADA,
    ];
    const enRevision = completados.includes(e);
    const asignado = [
      EstatusQueja.ASIGNADO_A_ABOGADO, EstatusQueja.EN_INVESTIGACION,
      EstatusQueja.PROCESO_CONCLUSION, EstatusQueja.FINALIZADA,
    ].includes(e);
    const finalizado = e === EstatusQueja.FINALIZADA;

    return [
      { label: 'Queja Recibida', fecha: this.queja.fechaRegistro, completado: true, activo: false },
      { label: 'En Revisión', fecha: enRevision ? this.queja.fechaRegistro : undefined, completado: enRevision, activo: e === EstatusQueja.EN_ANALISIS },
      { label: 'Asignación de Defensor', completado: asignado, activo: e === EstatusQueja.ASIGNADO_A_ABOGADO },
      { label: 'Resolución Final', completado: finalizado, activo: e === EstatusQueja.PROCESO_CONCLUSION },
    ];
  }

  ngOnInit(): void {
    const folio = this.route.snapshot.paramMap.get('folio')!;
    this.quejaService.seguimientoPrivado(folio).subscribe({
      next: (q) => {
        this.queja = q;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar el detalle de la queja.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  etiquetaEstatus(e: EstatusQueja): string {
    return ESTATUS_LABEL[e] ?? e;
  }

  /**
   * Descarga el acuse de recibo de la queja actual.
   * Usa el endpoint autenticado; el interceptor agrega el JWT automáticamente.
   */
  descargarAcuse(): void {
    if (!this.queja) return;
    this.quejaService.descargarAcuse(this.queja.folio).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `acuse-${this.queja!.folio}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  /**
   * Descarga una evidencia mediante HttpClient (el interceptor agrega el JWT).
   * Crea un enlace temporal en el DOM para forzar la descarga del blob.
   */
  descargar(ev: EvidenciaDTO): void {
    this.quejaService.descargarEvidencia(ev.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = ev.nombreArchivo;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }
}
