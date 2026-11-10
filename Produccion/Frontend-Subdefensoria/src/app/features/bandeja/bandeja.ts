import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { SesionPersonalService } from '../../core/services/sesion-personal.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ExpedienteResumen } from '../../core/models/expediente-resumen';
import { etiquetaEstatus } from '../../core/models/estatus-expediente';
import { ExpedienteResumenService } from '../../core/services/expediente-resumen.service';
import { AccionesExpedienteService } from '../../core/services/acciones-expediente.service';
import {
  GRUPOS, GrupoExpediente, ORDEN_NIVEL, SiguientePaso, claseEtapa, colorPlazo, enGrupo,
  formatearFecha, oficioVencido, porVencer, porcentajePlazo, siguientePaso, OPCIONES_ETAPA, etapasDe
} from '../../core/utils/etapas';
import { PRIORIDADES, clasePrioridad, codigoPrioridad, etiquetaPrioridad, rangoPrioridad } from '../../core/utils/prioridad';

type FiltroPlazo = '' | 'vencidos' | 'por-vencer' | 'sin-oficio';
type Orden = 'prioridad' | 'urgencia' | 'recientes' | 'antiguos';
const ORDEN_POR_DEFECTO: Orden = 'prioridad';

const POR_PAGINA = 10;

/** Mis expedientes: "¿Dónde va cada caso?" */
@Component({
  selector: 'app-bandeja',
  imports: [FormsModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.css'
})
export class Bandeja implements OnInit {
  /** 2026-11: el Defensor titular supervisa en solo consulta (sin acciones). */
  readonly soloConsulta = inject(SesionPersonalService).esDefensorTitular();

  readonly grupos = GRUPOS;
  readonly claseEtapa = claseEtapa;
  readonly colorPlazo = colorPlazo;
  readonly porcentajePlazo = porcentajePlazo;
  readonly formatearFecha = formatearFecha;
  readonly opcionesEtapa = OPCIONES_ETAPA;
  readonly prioridades = PRIORIDADES;
  readonly clasePrioridad = clasePrioridad;
  readonly etiquetaPrioridad = etiquetaPrioridad;
  private readonly destroyRef = inject(DestroyRef);

  todos: ExpedienteResumen[] = [];
  filtrados: ExpedienteResumen[] = [];
  pasos = new Map<number, SiguientePaso>();
  conteo: Record<GrupoExpediente, number> = { tramite: 0, atender: 0, ua: 0, conciliacion: 0, concluidos: 0 };
  unidades: string[] = [];

  grupo: GrupoExpediente = 'tramite';
  busqueda = '';
  unidad = '';
  plazo: FiltroPlazo = '';
  /** SD-2: una o varias etapas separadas por coma (ELABORO_ACUERDO,PENDIENTE_CONCLUSION). */
  etapa = '';
  /** SD-6: URGENTE | ALTA | MEDIA | BAJA | SIN_ASIGNAR. */
  prioridad = '';
  orden: Orden = ORDEN_POR_DEFECTO;
  pagina = 0;
  cargando = true;

  constructor(
    private expedienteResumenService: ExpedienteResumenService,
    private acciones: AccionesExpedienteService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // SD-2: se escucha la URL (no solo al entrar), así la flecha atrás y los cuadros de
    // Inicio siempre dejan la bandeja en el mismo filtro.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(q => {
      this.leerUrl(q);
      if (!this.cargando) {
        this.aplicarFiltros();
        this.cdr.detectChanges();
      }
    });
    this.cargar();
  }

  private leerUrl(q: ParamMap): void {
    const grupo = q.get('grupo') as GrupoExpediente | null;
    this.grupo = grupo && GRUPOS.some(g => g.id === grupo) ? grupo : 'tramite';
    this.plazo = (q.get('plazo') as FiltroPlazo) ?? '';
    this.etapa = q.get('etapa') ?? '';
    this.unidad = q.get('unidad') ?? '';
    this.prioridad = q.get('prioridad') ?? '';
    const orden = q.get('orden') as Orden | null;
    this.orden = orden && ['prioridad', 'urgencia', 'recientes', 'antiguos'].includes(orden) ? orden : ORDEN_POR_DEFECTO;
    // Una etapa "Concluido" sola no tiene sentido en "En trámite".
    if (etapasDe(this.etapa).length === 1 && this.etapa === 'CONCLUIDO' && this.grupo === 'tramite') this.grupo = 'concluidos';
  }

  /** Guarda pestaña y filtros en la URL sin crear otra entrada en el historial. */
  private escribirUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        grupo: this.grupo === 'tramite' ? null : this.grupo,
        etapa: this.etapa || null,
        plazo: this.plazo || null,
        unidad: this.unidad || null,
        prioridad: this.prioridad || null,
        orden: this.orden === ORDEN_POR_DEFECTO ? null : this.orden
      }
    });
  }

  /** Cambio en un desplegable: aplica y lo deja en la URL. */
  cambiarFiltro(): void {
    if (this.etapa === 'CONCLUIDO' && this.grupo === 'tramite') this.grupo = 'concluidos';
    else if (this.etapa && this.etapa !== 'CONCLUIDO' && this.grupo === 'concluidos') this.grupo = 'tramite';
    this.aplicarFiltros();
    this.escribirUrl();
  }

  /** Etapa elegida que no está en la lista (llegó por URL con otra combinación). */
  get etapaFueraDeLista(): boolean {
    return !!this.etapa && !OPCIONES_ETAPA.some(o => o.valor === this.etapa);
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
    // Pestaña y etapa se combinan; si la etapa elegida no cabe en la pestaña, se quita.
    const cabe = (est: string) => grupo === 'tramite' ? est !== 'CONCLUIDO'
      : grupo === 'concluidos' ? est === 'CONCLUIDO'
      : grupo === 'ua' ? est === 'EN_ESPERA_OFICIO'
      : grupo === 'conciliacion' ? est === 'PENDIENTE_CONCILIACION' : true;
    const etapas = etapasDe(this.etapa);
    if (etapas.length && !etapas.some(cabe)) this.etapa = '';
    this.aplicarFiltros();
    this.escribirUrl();
  }

  aplicarFiltros(): void {
    const texto = this.busqueda.trim().toLowerCase();
    const etapas = etapasDe(this.etapa);

    this.filtrados = this.todos.filter(e => {
      if (!enGrupo(e, this.grupo)) return false;
      if (etapas.length && !etapas.includes(e.estatus)) return false;
      if (this.unidad && e.unidadAcademica !== this.unidad) return false;
      if (this.prioridad && codigoPrioridad(e.prioridad) !== this.prioridad) return false;
      if (this.plazo === 'vencidos' && !oficioVencido(e)) return false;
      if (this.plazo === 'por-vencer' && !porVencer(e)) return false;
      if (this.plazo === 'sin-oficio' && e.numeroOficioVigente) return false;
      return !texto || [e.folio, e.quejosoNombre, e.asunto, e.unidadAcademica, e.numeroOficioVigente]
        .some(v => (v ?? '').toLowerCase().includes(texto));
    }).sort((a, b) => {
      // SD-6: Urgente -> Alta -> Media -> Baja -> Sin asignar; empate por urgencia y fecha.
      if (this.orden === 'prioridad') {
        const p = rangoPrioridad(a.prioridad) - rangoPrioridad(b.prioridad);
        if (p) return p;
      }
      if (this.orden === 'urgencia' || this.orden === 'prioridad') {
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
    const columnas = ['Folio', 'Quejoso', 'Asunto', 'Unidad', 'Admisión', 'Prioridad', 'Etapa', 'Oficio vigente', 'Días transcurridos', 'Días de plazo', 'Siguiente paso'];
    const filas = this.filtrados.map(e => [
      e.folio, e.quejosoNombre, e.asunto, e.unidadAcademica, e.fechaAdmision, etiquetaPrioridad(e.prioridad), this.etiquetaEtapa(e),
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
