import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { etiquetaEstatus } from '../../core/models/estatus-expediente';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { AccionesExpedienteService } from '../../core/services/acciones-expediente.service';
import {
  GRUPOS, GrupoExpediente, ORDEN_NIVEL, SiguientePaso, claseEtapa, colorPlazo, enGrupo,
  formatearFecha, oficioVencido, porVencer, porcentajePlazo, siguientePaso
} from '../../core/utils/etapas';

type FiltroPlazo = '' | 'vencidos' | 'por-vencer' | 'sin-oficio';
type Orden = 'urgencia' | 'recientes' | 'antiguos';

const POR_PAGINA = 10;

/** Mis expedientes: "¿Dónde va cada caso?" */
@Component({
  selector: 'app-bandeja',
  imports: [FormsModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.css'
})
export class Bandeja implements OnInit {
  readonly grupos = GRUPOS;
  readonly claseEtapa = claseEtapa;
  readonly colorPlazo = colorPlazo;
  readonly porcentajePlazo = porcentajePlazo;
  readonly formatearFecha = formatearFecha;

  todos: ExpedienteResumen[] = [];
  filtrados: ExpedienteResumen[] = [];
  pasos = new Map<number, SiguientePaso>();
  conteo: Record<GrupoExpediente, number> = { tramite: 0, atender: 0, ua: 0, conciliacion: 0, concluidos: 0 };
  unidades: string[] = [];

  grupo: GrupoExpediente = 'tramite';
  busqueda = '';
  unidad = '';
  plazo: FiltroPlazo = '';
  orden: Orden = 'urgencia';
  pagina = 0;
  cargando = true;

  constructor(
    private expedienteResumenService: ExpedienteResumenService,
    private acciones: AccionesExpedienteService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    const grupo = q.get('grupo') as GrupoExpediente | null;
    if (grupo && GRUPOS.some(g => g.id === grupo)) this.grupo = grupo;
    this.plazo = (q.get('plazo') as FiltroPlazo) ?? '';
    this.cargar();
  }

  cargar(): void {
    this.expedienteResumenService.listarTodos().subscribe({
      next: (items) => {
        this.todos = items;
        this.pasos = new Map(items.map(e => [e.expedienteId, siguientePaso(e)]));
        for (const g of GRUPOS) this.conteo[g.id] = items.filter(e => enGrupo(e, g.id)).length;
        this.unidades = [...new Set(items.map(e => e.unidadAcademica).filter(Boolean))].sort();
        this.cargando = false;
        this.aplicarFiltros();
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar tus expedientes.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  elegirGrupo(grupo: GrupoExpediente): void {
    this.grupo = grupo;
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    const texto = this.busqueda.trim().toLowerCase();

    this.filtrados = this.todos.filter(e => {
      if (!enGrupo(e, this.grupo)) return false;
      if (this.unidad && e.unidadAcademica !== this.unidad) return false;
      if (this.plazo === 'vencidos' && !oficioVencido(e)) return false;
      if (this.plazo === 'por-vencer' && !porVencer(e)) return false;
      if (this.plazo === 'sin-oficio' && e.numeroOficioVigente) return false;
      return !texto || [e.folio, e.quejosoNombre, e.asunto, e.unidadAcademica, e.numeroOficioVigente]
        .some(v => (v ?? '').toLowerCase().includes(texto));
    }).sort((a, b) => {
      if (this.orden === 'urgencia') {
        const diferencia = ORDEN_NIVEL[this.paso(a).nivel] - ORDEN_NIVEL[this.paso(b).nivel];
        if (diferencia) return diferencia;
      }
      const fa = a.fechaAdmision ?? '';
      const fb = b.fechaAdmision ?? '';
      return this.orden === 'recientes' ? fb.localeCompare(fa) : fa.localeCompare(fb);
    });

    this.pagina = 0;
  }

  paso(e: ExpedienteResumen): SiguientePaso {
    return this.pasos.get(e.expedienteId) ?? siguientePaso(e);
  }

  etiquetaEtapa(e: ExpedienteResumen): string {
    return oficioVencido(e) ? 'Oficio vencido' : etiquetaEstatus(e.estatus);
  }

  get visibles(): ExpedienteResumen[] {
    return this.filtrados.slice(this.pagina * POR_PAGINA, (this.pagina + 1) * POR_PAGINA);
  }

  get paginas(): number[] {
    return Array.from({ length: Math.ceil(this.filtrados.length / POR_PAGINA) }, (_, i) => i);
  }

  get rango(): string {
    if (!this.filtrados.length) return 'Sin resultados';
    const desde = this.pagina * POR_PAGINA + 1;
    return `Mostrando ${desde}–${Math.min(desde + POR_PAGINA - 1, this.filtrados.length)} de ${this.filtrados.length}`;
  }

  hacer(e: ExpedienteResumen, evento: Event): void {
    evento.stopPropagation();
    this.acciones.ejecutar(e, this.paso(e).accion).subscribe(cambio => {
      if (cambio) this.cargar();
    });
  }

  abrir(e: ExpedienteResumen): void {
    this.acciones.abrir(e);
  }

  exportarCsv(): void {
    const columnas = ['Folio', 'Quejoso', 'Asunto', 'Unidad', 'Admisión', 'Etapa', 'Oficio vigente', 'Días transcurridos', 'Días de plazo', 'Siguiente paso'];
    const filas = this.filtrados.map(e => [
      e.folio, e.quejosoNombre, e.asunto, e.unidadAcademica, e.fechaAdmision, this.etiquetaEtapa(e),
      e.numeroOficioVigente ?? '', e.diasTranscurridos ?? '', e.diasLimite ?? '', this.paso(e).descripcion
    ]);
    const csv = [columnas, ...filas]
      .map(f => f.map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))
      .join('\r\n');

    const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `expedientes-subdefensoria-${new Date().toISOString().slice(0, 10)}.csv`;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }
}
