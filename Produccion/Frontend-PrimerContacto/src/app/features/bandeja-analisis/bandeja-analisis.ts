import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, Subscription, debounceTime, switchMap } from 'rxjs';
import { ExpedienteBandeja, FiltroBandeja } from '../../core/models/expediente-bandeja';
import { BandejaService } from '../../core/services/bandeja.service';
import {
  claseEstatus,
  clasePrioridad,
  codigoPrioridad,
  etiquetaEstatus,
  PrioridadEtiqueta
} from '../../core/utils/estatus-expediente';
import { Paginado, diasDesde, haceCuanto } from '../../core/utils/paginado';

interface FiltrosBandeja {
  texto: string;
  orden: 'recientes' | 'antiguos';
  /** '' = todas. Un solo valor por filtro (listas desplegables, como en Recepción). */
  prioridad: string;
  tema: string;
  escuela: string;
  /** Código del backend (EN_ANALISIS...), no la etiqueta. */
  estatus: string;
}

/** Orden del diagrama de estados, para listar el filtro de estatus. */
const ORDEN_ESTATUS = ['TURNADA', 'EN_ANALISIS', 'PROCEDENTE', 'IMPROCEDENTE', 'REMITIDA'];

/** Valores del filtro de estatus que agrupan varios códigos (los usan los cuadros). */
const ESTATUS_ABIERTOS = 'ABIERTOS';
const ESTATUS_DICTAMINADOS = 'DICTAMINADOS';
const GRUPOS_ESTATUS: Record<string, string[]> = {
  [ESTATUS_ABIERTOS]: ['TURNADA', 'EN_ANALISIS'],
  [ESTATUS_DICTAMINADOS]: ['PROCEDENTE', 'IMPROCEDENTE']
};

const FILTROS_VACIOS: FiltrosBandeja = {
  texto: '',
  orden: 'recientes',
  prioridad: '',
  tema: '',
  escuela: '',
  estatus: ''
};

/**
 * Bandeja de Análisis (rediseño 2026-11, homologada con Recepción): barra de filtros arriba,
 * pestañas por prioridad, tabla a todo el ancho y paginado en el navegador. Los RESULTADOS los
 * filtra el backend (CU-PC-02); la lista completa solo sirve para opciones y conteos.
 */
@Component({
  selector: 'app-bandeja-analisis',
  imports: [FormsModule, MatTooltipModule, MatSnackBarModule],
  templateUrl: './bandeja-analisis.html',
  styleUrls: ['../../shared/estilos/recepcion.css', './bandeja-analisis.css']
})
export class BandejaAnalisis implements OnInit, OnDestroy {

  constructor(
    private router: Router,
    private bandejaService: BandejaService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  readonly etiquetaEstatus = etiquetaEstatus;
  readonly claseEstatus = claseEstatus;
  readonly clasePrioridad = clasePrioridad;
  readonly haceCuanto = haceCuanto;
  readonly ESTATUS_ABIERTOS = ESTATUS_ABIERTOS;
  readonly ESTATUS_DICTAMINADOS = ESTATUS_DICTAMINADOS;

  readonly prioridades: PrioridadEtiqueta[] = ['Urgente', 'Alta', 'Media', 'Baja', 'Sin asignar'];

  /** Pestañas rápidas: atajos del filtro de prioridad. */
  readonly pestanas: { etiqueta: string; prioridad: string }[] = [
    { etiqueta: 'Todos', prioridad: '' },
    { etiqueta: 'Sin asignar', prioridad: 'Sin asignar' },
    { etiqueta: 'Urgentes', prioridad: 'Urgente' },
    { etiqueta: 'Alta prioridad', prioridad: 'Alta' }
  ];

  /** Resultado del backend con los filtros aplicados. */
  resultados: ExpedienteBandeja[] = [];
  private expedientesOriginal: ExpedienteBandeja[] = [];
  cargando = true;
  error = false;

  readonly paginado = new Paginado<ExpedienteBandeja>(10);
  readonly tamaniosPagina = [10, 20, 50];

  filtros: FiltrosBandeja = { ...FILTROS_VACIOS };

  private readonly cambios$ = new Subject<void>();
  private suscripcion?: Subscription;

  ngOnInit(): void {
    this.suscripcion = this.cambios$
      .pipe(
        debounceTime(250),
        switchMap(() => this.bandejaService.filtrar(this.construirFiltro()))
      )
      .subscribe({
        next: (resultado) => {
          this.resultados = resultado;
          this.paginado.pagina = 0;
          this.cdr.detectChanges();
        },
        error: () => {
          this.snackBar.open('No fue posible aplicar los filtros.', 'Cerrar', { duration: 3000 });
        }
      });

    this.cargarBandeja();
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
  }

  cargarBandeja(): void {
    this.cargando = true;
    this.error = false;
    this.bandejaService.obtenerBandeja().subscribe({
      next: (expedientes) => {
        this.expedientesOriginal = expedientes;
        this.resultados = expedientes;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar la bandeja de análisis.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  // ---------------------------------------------------------------- opciones y conteos

  get temas(): string[] {
    return this.valoresDe('tema');
  }

  get escuelas(): string[] {
    return this.valoresDe('unidadAcademica');
  }

  get estatusDisponibles(): string[] {
    return Array.from(new Set(this.expedientesOriginal.map(e => e.estatusCodigo)))
      .filter(Boolean)
      .sort((a, b) => ORDEN_ESTATUS.indexOf(a) - ORDEN_ESTATUS.indexOf(b));
  }

  // ---------------------------------------------------------------- indicadores (2026-11)
  // Mismo patrón que el Historial de Recepción: se calculan con los expedientes asignados
  // y, al picarlos, filtran la tabla.

  private contar(...codigos: string[]): number {
    return this.expedientesOriginal.filter(e => codigos.includes(e.estatusCodigo)).length;
  }

  get totalAsignados(): number {
    return this.expedientesOriginal.length;
  }

  get porAbrir(): number {
    return this.contar('TURNADA');
  }

  get enAnalisis(): number {
    return this.contar('EN_ANALISIS');
  }

  get enAnalisisConCita(): number {
    return this.expedientesOriginal.filter(e => e.estatusCodigo === 'EN_ANALISIS' && e.tieneCitaActiva).length;
  }

  get dictaminados(): number {
    return this.contar('PROCEDENTE', 'IMPROCEDENTE');
  }

  get porcentajeProcedentes(): number {
    return this.dictaminados ? Math.round((this.contar('PROCEDENTE') / this.dictaminados) * 100) : 0;
  }

  /** Días que llevan, en promedio y el más viejo, los expedientes abiertos (Turnada o En análisis). */
  get antiguedad(): { promedio: number; maximo: number } | null {
    const dias = this.expedientesOriginal
      .filter(e => e.estatusCodigo === 'TURNADA' || e.estatusCodigo === 'EN_ANALISIS')
      .map(e => diasDesde(e.fechaRecepcion))
      .filter((d): d is number => d !== null);
    if (!dias.length) return null;
    return {
      promedio: Math.round(dias.reduce((a, b) => a + b, 0) / dias.length),
      maximo: Math.max(...dias)
    };
  }

  /** Cuadro seleccionado: el filtro de estatus que tiene aplicado ('' = todos). */
  filtrarPorEstatus(estatus: string): void {
    this.filtros = { ...FILTROS_VACIOS, orden: this.filtros.orden, estatus };
    this.aplicarFiltros();
  }

  verMasAntiguos(): void {
    this.filtros = { ...FILTROS_VACIOS, orden: 'antiguos', estatus: ESTATUS_ABIERTOS };
    this.aplicarFiltros();
  }

  conteoPrioridad(prioridad: string): number {
    if (!prioridad) return this.expedientesOriginal.length;
    return this.expedientesOriginal.filter(e => e.prioridad === prioridad).length;
  }

  get hayFiltros(): boolean {
    const f = this.filtros;
    return !!(f.texto.trim() || f.prioridad || f.tema || f.escuela || f.estatus);
  }

  // ---------------------------------------------------------------- acciones

  elegirPestana(prioridad: string): void {
    this.filtros.prioridad = prioridad;
    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    this.cambios$.next();
  }

  limpiarFiltros(): void {
    this.filtros = { ...FILTROS_VACIOS, orden: this.filtros.orden };
    this.aplicarFiltros();
  }

  analizar(expediente: ExpedienteBandeja): void {
    this.router.navigate(['/expediente', expediente.folio]);
  }

  // ---------------------------------------------------------------- paginado

  get filasPagina(): ExpedienteBandeja[] {
    return this.paginado.cortar(this.resultados);
  }

  irAPagina(p: number): void {
    this.paginado.irA(p, this.resultados.length);
  }

  cambiarTamanio(): void {
    this.paginado.pagina = 0;
  }

  // ---------------------------------------------------------------- privados

  private construirFiltro(): FiltroBandeja {
    const f = this.filtros;
    return {
      texto: f.texto.trim() || undefined,
      prioridades: f.prioridad ? [codigoPrioridad(f.prioridad)] : [],
      estatusLista: GRUPOS_ESTATUS[f.estatus] ?? (f.estatus ? [f.estatus] : []),
      unidadesAcademicas: f.escuela ? [f.escuela] : [],
      temas: f.tema ? [f.tema] : [],
      orden: f.orden
    };
  }

  private valoresDe(campo: 'tema' | 'unidadAcademica'): string[] {
    return Array.from(new Set(this.expedientesOriginal.map(e => e[campo]).filter(Boolean)))
      .sort((a, b) => a.localeCompare(b, 'es'));
  }
}
