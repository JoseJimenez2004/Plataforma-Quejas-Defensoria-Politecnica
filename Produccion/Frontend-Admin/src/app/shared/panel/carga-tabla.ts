import { Component, Input, OnChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CargaTrabajo, ETIQUETA_NIVEL, MiembroCarga } from '../../core/models/admin.models';
import { Paginador } from './paginador';
import { descargarCsv, fechaHora } from './csv';

/**
 * Carga de trabajo por integrante, en dos pestañas (Recepcionistas / Personal jurídico).
 * Recibe los datos ya cargados; filtra y pagina en el navegador porque son pocas personas.
 */
@Component({
  selector: 'app-carga-tabla',
  standalone: true,
  imports: [FormsModule, Paginador],
  templateUrl: './carga-tabla.html',
})
export class CargaTabla implements OnChanges {
  @Input() datos: CargaTrabajo | null = null;
  @Input() tamanio = 5;
  @Input() completa = false;

  pestana: 'recep' | 'abog' = 'recep';
  texto = '';
  estatus = '';
  nivel = '';
  pagina = 0;
  readonly etiquetaNivel = ETIQUETA_NIVEL;
  readonly fechaHora = fechaHora;

  constructor(private router: Router) {}

  ngOnChanges(): void {
    this.pagina = 0;
  }

  get lista(): MiembroCarga[] {
    return (this.pestana === 'recep' ? this.datos?.recepcionistas : this.datos?.abogados) ?? [];
  }

  get filtradas(): MiembroCarga[] {
    const t = this.texto.trim().toLowerCase();
    return this.lista.filter((m) =>
      (!t || m.nombre.toLowerCase().includes(t) || (m.correo ?? '').toLowerCase().includes(t))
      && (!this.estatus || (this.estatus === 'ACTIVO') === m.activo)
      && (!this.nivel || m.nivel === this.nivel));
  }

  get visibles(): MiembroCarga[] {
    return this.filtradas.slice(this.pagina * this.tamanio, (this.pagina + 1) * this.tamanio);
  }

  get maxCarga(): number {
    return Math.max(1, ...this.lista.map((m) => m.carga));
  }

  cambiarPestana(p: 'recep' | 'abog'): void {
    this.pestana = p;
    this.pagina = 0;
  }

  aplicar(): void {
    this.pagina = 0;
  }

  limpiar(): void {
    this.texto = '';
    this.estatus = '';
    this.nivel = '';
    this.pagina = 0;
  }

  verQuejas(m: MiembroCarga): void {
    const queryParams = this.pestana === 'recep' ? { recepcionista: m.correo } : { abogado: m.nombre };
    this.router.navigate(['/quejas'], { queryParams });
  }

  exportar(): void {
    const r = this.datos?.recepcionistas ?? [];
    const a = this.datos?.abogados ?? [];
    descargarCsv('carga-de-trabajo', ['Área', 'Nombre', 'Correo', 'Abiertas / asignadas', 'En trámite', 'Atendidas / concluidas', 'Turnadas', 'Observaciones',
      'En Primer Contacto', 'En Subdefensoría', 'Última actividad', 'Carga', '% del equipo', 'Nivel'],
    [...r.map((m) => ['Recepción', m.nombre, m.correo, m.abiertas, m.enTramite, m.atendidas, m.turnadas, m.observaciones, '', '',
      fechaHora(m.ultimaActividad), m.carga, m.porcentaje + '%', ETIQUETA_NIVEL[m.nivel]]),
    ...a.map((m) => ['Jurídico', m.nombre, m.correo, m.abiertas, m.enTramite, m.cerradas, '', '', m.enPrimerContacto, m.enSubdefensoria,
      fechaHora(m.ultimaActividad), m.carga, m.porcentaje + '%', ETIQUETA_NIVEL[m.nivel]])]);
  }
}
