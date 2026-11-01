import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPlaceholder, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { NgTemplateOutlet } from '@angular/common';
import { forkJoin } from 'rxjs';

import { SeguimientoService } from '../../core/services/seguimiento.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthAdminService } from '../../core/services/auth-admin.service';
import { FilaQuejaSeg, PERIODOS, PanelDefensora, variacion } from '../../core/models/admin.models';
import { TitularIpn, titularDe, FECHA_DIRECTORIO_IPN } from '../../core/data/titulares-ipn';
import { ModalPanel } from '../../shared/panel/modal-panel';
import { Barras, SerieBarras } from '../../shared/panel/barras';
import { AtencionTabla } from '../../shared/panel/atencion-tabla';
import { MotivosTabla } from '../../shared/panel/motivos-tabla';
import { COLOR_ETAPA, ETIQUETA_ETAPA_CORTA } from '../../shared/panel/colores';
import { AYUDA, AyudaSeccion, NOMBRE_SECCION, ORDEN_INICIAL, PASOS_RECORRIDO, SeccionPanel } from './ayuda-panel';

/** Dimensiones por las que se puede filtrar el panel. */
type Dim =
  | 'etapa' | 'mes' | 'unidad' | 'nivel' | 'rama' | 'edad'
  | 'origen' | 'prioridad' | 'recepcionista' | 'abogado' | 'dia' | 'hora';

type Ventana = 'atencion' | 'motivos' | null;

/** Filtros: cada dimensión admite varios valores (se combinan con "o"; dimensiones distintas con "y"). */
type Filtros = Partial<Record<Dim, string[]>>;

/** Todo lo que define una vista del panel (se guarda en "Mis vistas" y viaja en la URL). */
interface EstadoPanel {
  filtros: Filtros;
  dias: number;
  desde?: string;
  hasta?: string;
  q?: string;
}

interface VistaGuardada {
  nombre: string;
  estado: EstadoPanel;
  fija?: boolean;
}

/** Una queja con los campos derivados que usan los cruces. */
interface Fila extends FilaQuejaSeg {
  ms: number;
  mesClave: string;
  nivel: string;
  rama: string;
  edadClave: string;
  diaClave: string;
  horaClave: string;
  recep: string;
  abog: string;
  prio: string;
  abierta: boolean;
  texto: string;
}

interface Conteo {
  clave: string;
  etiqueta: string;
  total: number;
  abiertas: number;
}

interface FilaCalor {
  clave: string;
  etiqueta: string;
  total: number;
  anterior: number;
  celdas: { clave: string; valor: number }[];
}

interface OpcionFiltro {
  clave: string;
  etiqueta: string;
  total: number;
}

const DIMS: Dim[] = ['etapa', 'unidad', 'nivel', 'rama', 'edad', 'mes', 'dia', 'hora', 'origen', 'prioridad', 'recepcionista', 'abogado'];
const ETAPAS = ['RECEPCION', 'CON_QUEJOSO', 'PRIMER_CONTACTO', 'SUBDEFENSORIA', 'CERRADA'];
const ETAPAS_ABIERTAS = ETAPAS.filter((e) => e !== 'CERRADA');

const EDADES: { clave: string; etiqueta: string; min: number; max: number }[] = [
  { clave: '0-3', etiqueta: '0–3 días', min: 0, max: 3 },
  { clave: '4-7', etiqueta: '4–7 días', min: 4, max: 7 },
  { clave: '8-15', etiqueta: '8–15 días', min: 8, max: 15 },
  { clave: '16-30', etiqueta: '16–30 días', min: 16, max: 30 },
  { clave: '31-60', etiqueta: '31–60 días', min: 31, max: 60 },
  { clave: '60+', etiqueta: 'Más de 60', min: 61, max: Number.MAX_SAFE_INTEGER },
];

const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const DIAS_LARGOS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábados', 'domingos'];
const HORAS = Array.from({ length: 12 }, (_, i) => String(i * 2).padStart(2, '0'));
const MESES_CORTOS = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

const ETIQUETA_DIM: Record<Dim, string> = {
  etapa: 'Etapa', mes: 'Mes de llegada', unidad: 'Unidad', nivel: 'Nivel', rama: 'Rama', edad: 'Antigüedad',
  origen: 'Origen', prioridad: 'Prioridad', recepcionista: 'Recepcionista', abogado: 'Abogado',
  dia: 'Día de llegada', hora: 'Horario de llegada',
};

const RAMA_CORTA: Record<string, string> = {
  'Ingeniería y Ciencias Físico Matemáticas': 'Ingeniería y C. Físico Matemáticas',
  'Ciencias Médico Biológicas': 'Médico Biológicas',
  'Ciencias Sociales y Administrativas': 'Sociales y Administrativas',
  Interdisciplinaria: 'Interdisciplinarias',
};

const SIN_RECEPCIONISTA = 'Nadie la ha tomado';
const SIN_ABOGADO = 'Sin asignar';
const SIN_PRIORIDAD = 'Sin prioridad';
const SIN_CLASIFICAR = 'Sin clasificar';
const TOP_UNIDADES = 12;
const PERIODO_PERSONALIZADO = -1;

const CLAVE_VISTAS = 'ddp_admin_panel_vistas';
const CLAVE_DISENO = 'ddp_admin_panel_diseno';
const CLAVE_RECORRIDO = 'ddp_admin_panel_recorrido_visto';

/** Vistas de ejemplo para empezar (no se pueden borrar). */
const VISTAS_SUGERIDAS: VistaGuardada[] = [
  { nombre: 'Rezago en Recepción (nadie la ha tomado)', fija: true, estado: { dias: 0, filtros: { etapa: ['RECEPCION'], recepcionista: [SIN_RECEPCIONISTA] } } },
  { nombre: 'Abiertas con más de 30 días', fija: true, estado: { dias: 0, filtros: { edad: ['31-60', '60+'] } } },
  { nombre: 'Primer Contacto · prioridad alta', fija: true, estado: { dias: 0, filtros: { etapa: ['PRIMER_CONTACTO'], prioridad: ['Alta'] } } },
  { nombre: 'Nivel Medio Superior (CECyT)', fija: true, estado: { dias: 365, filtros: { nivel: ['Medio Superior'] } } },
  { nombre: 'Registradas en ventanilla este mes', fija: true, estado: { dias: 30, filtros: { origen: ['Ventanilla'] } } },
];

/**
 * Panel de la Defensora — tablero interactivo (2026-10-28, v2).
 *
 * Descarga una vez todas las quejas y hace los cruces en el navegador. Cualquier clic filtra todo
 * el panel; Ctrl/⌘ + clic (o "Selección múltiple") suma valores. Además: panel de filtros con
 * listas, rango de fechas y búsqueda; vistas guardadas; filtros en la URL para compartir;
 * secciones que se arrastran u ocultan; focos de ayuda y recorrido guiado.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, NgTemplateOutlet, ModalPanel, Barras, AtencionTabla, MotivosTabla, CdkDropList, CdkDrag, CdkDragHandle, CdkDragPlaceholder],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  readonly periodos = [...PERIODOS, { dias: PERIODO_PERSONALIZADO, etiqueta: 'Rango personalizado…' }];
  readonly PERSONALIZADO = PERIODO_PERSONALIZADO;
  readonly colorEtapa = COLOR_ETAPA;
  readonly etiquetaEtapa = ETIQUETA_ETAPA_CORTA;
  readonly etapas = ETAPAS;
  readonly etapasAbiertas = ETAPAS_ABIERTAS;
  readonly diasSemana = DIAS_SEMANA;
  readonly horas = HORAS;
  readonly fechaDirectorio = FECHA_DIRECTORIO_IPN;
  readonly dims = DIMS;
  readonly etiquetaDim = ETIQUETA_DIM;
  readonly ayuda = AYUDA;
  readonly nombreSeccion = NOMBRE_SECCION;
  readonly pasos = PASOS_RECORRIDO;
  readonly vistasSugeridas = VISTAS_SUGERIDAS;

  panel: PanelDefensora | null = null;
  todas: Fila[] = [];
  cargando = true;
  ventana: Ventana = null;

  // ---- estado de la vista
  dias = 30;
  desde = '';
  hasta = '';
  q = '';
  filtros: Filtros = {};
  multiple = false;

  // ---- interfaz
  panelFiltros = false;
  buscaOpcion: Partial<Record<Dim, string>> = {};
  menuVistas = false;
  vistas: VistaGuardada[] = [];
  nombreVista = '';
  ayudaAbierta: string | null = null;
  modoAyuda = false;
  menuAyuda = false;
  paso: number | null = null;
  personalizando = false;
  orden: SeccionPanel[] = [...ORDEN_INICIAL];
  ocultas = new Set<SeccionPanel>();

  // ---- resultados del último cálculo
  vista: Fila[] = [];
  basePeriodo: Fila[] = [];
  periodoAnterior: Fila[] = [];
  porEtapa: { clave: string; total: number; estatus: { etiqueta: string; total: number }[] }[] = [];
  etiquetasMes: string[] = [];
  clavesMes: string[] = [];
  seriesMes: SerieBarras[] = [];
  etiquetasEdad = EDADES.map((e) => e.etiqueta);
  seriesEdad: SerieBarras[] = [];
  calorUnidades: FilaCalor[] = [];
  maxCalorUnidad = 1;
  calorDias: FilaCalor[] = [];
  maxCalorDia = 1;
  niveles: Conteo[] = [];
  ramas: Conteo[] = [];
  origenes: Conteo[] = [];
  prioridades: Conteo[] = [];
  recepcionistas: Conteo[] = [];
  abogados: Conteo[] = [];
  antiguas: Fila[] = [];
  hallazgos: { texto: string; tipo: 'info' | 'alerta' | 'bien' }[] = [];
  opciones: Partial<Record<Dim, OpcionFiltro[]>> = {};
  ficha: {
    clave: string;
    nombre: string;
    titular?: TitularIpn;
    total: number;
    anterior: number;
    abiertas: number;
    pctPeriodo: number;
    lugar: number;
    deUnidades: number;
    mediana: number;
    medianaGeneral: number;
    etapas: { clave: string; total: number }[];
  } | null = null;

  readonly fechaHoy = (() => {
    const t = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date());
    return t.charAt(0).toUpperCase() + t.slice(1);
  })();

  constructor(
    private seguimiento: SeguimientoService,
    private auth: AuthAdminService,
    private route: ActivatedRoute,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.leerDiseno();
    this.vistas = this.leerLocal<VistaGuardada[]>(CLAVE_VISTAS, []);
    this.aplicarEstado(this.estadoDesdeUrl(), false);
    this.cargar();
  }

  get titulo(): string {
    return this.auth.usuarioActual()?.rol === 'DEFENSOR' ? 'Panel de la Defensora' : 'Panel general';
  }

  get etiquetaPeriodo(): string {
    if (this.dias === PERIODO_PERSONALIZADO) {
      return `del ${this.fechaCorta(this.desde) || 'inicio'} al ${this.fechaCorta(this.hasta) || 'hoy'}`;
    }
    return this.dias === 0 ? 'en todo el historial' : `en los últimos ${this.dias} días`;
  }

  // ====================================================================== carga

  cargar(): void {
    this.cargando = !this.panel;
    forkJoin({
      panel: this.seguimiento.tablero(this.diasServidor, 12),
      quejas: this.seguimiento.quejas({}, 0, 5000),
    }).subscribe({
      next: ({ panel, quejas }) => {
        this.panel = panel;
        this.todas = quejas.filas.map((f) => this.enriquecer(f));
        this.cargando = false;
        this.recalcular();
        this.cdr.detectChanges();
        if (!this.leerLocal<boolean>(CLAVE_RECORRIDO, false)) {
          setTimeout(() => this.iniciarRecorrido(), 600);
        }
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudo cargar el panel general.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Los casos y motivos vienen del servidor, que solo entiende "últimos N días". */
  private get diasServidor(): number {
    return this.dias === PERIODO_PERSONALIZADO ? 0 : this.dias;
  }

  cambiarPeriodo(): void {
    if (this.dias !== PERIODO_PERSONALIZADO) {
      this.desde = '';
      this.hasta = '';
    }
    this.seguimiento.tablero(this.diasServidor, 12).subscribe({
      next: (p) => {
        this.panel = p;
        this.cdr.detectChanges();
      },
    });
    this.alCambiar();
  }

  private enriquecer(f: FilaQuejaSeg): Fila {
    const fecha = new Date(f.fecha);
    const meta = titularDe(f.unidadClave);
    const abierta = f.etapa !== 'CERRADA';
    const edad = EDADES.find((e) => f.dias >= e.min && f.dias <= e.max) ?? EDADES[EDADES.length - 1];
    return {
      ...f,
      ms: fecha.getTime(),
      mesClave: `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`,
      nivel: meta?.nivel ?? SIN_CLASIFICAR,
      rama: meta?.rama ? (RAMA_CORTA[meta.rama] ?? meta.rama) : (meta ? 'No aplica' : SIN_CLASIFICAR),
      edadClave: abierta ? edad.clave : 'CERRADA',
      diaClave: String((fecha.getDay() + 6) % 7),
      horaClave: HORAS[Math.floor(fecha.getHours() / 2)],
      recep: f.recepcionista?.trim() || SIN_RECEPCIONISTA,
      abog: f.abogado?.trim() || SIN_ABOGADO,
      prio: f.prioridad ? f.prioridad.charAt(0) + f.prioridad.slice(1).toLowerCase() : SIN_PRIORIDAD,
      abierta,
      texto: this.normalizar(`${f.folio} ${f.quejoso} ${f.unidadNombre ?? ''} ${f.abogado ?? ''}`),
    };
  }

  // ====================================================================== filtros

  private valorDe(f: Fila, d: Dim): string {
    switch (d) {
      case 'etapa': return f.etapa;
      case 'mes': return f.mesClave;
      case 'unidad': return f.unidadClave ?? '';
      case 'nivel': return f.nivel;
      case 'rama': return f.rama;
      case 'edad': return f.edadClave;
      case 'origen': return f.origen;
      case 'prioridad': return f.prio;
      case 'recepcionista': return f.recep;
      case 'abogado': return f.abog;
      case 'dia': return f.diaClave;
      case 'hora': return f.horaClave;
    }
  }

  private cumple(filas: Fila[], excluir: Dim[]): Fila[] {
    const activos = (Object.keys(this.filtros) as Dim[]).filter((d) => !excluir.includes(d) && this.filtros[d]?.length);
    const t = this.normalizar(this.q.trim());
    if (!activos.length && !t) {
      return filas;
    }
    return filas.filter((f) => (!t || f.texto.includes(t)) && activos.every((d) => this.filtros[d]!.includes(this.valorDe(f, d))));
  }

  /** Quejas del periodo con todos los filtros, salvo los de {@code excluir}. */
  private base(...excluir: Dim[]): Fila[] {
    return this.cumple(this.basePeriodo, excluir);
  }

  private baseAnterior(...excluir: Dim[]): Fila[] {
    return this.cumple(this.periodoAnterior, excluir);
  }

  /**
   * Clic en un elemento. Clic normal: deja solo ese valor (o lo quita si ya era el único).
   * Ctrl/⌘/Mayús + clic, o modo "Selección múltiple": lo suma o lo quita de la selección.
   */
  alternar(d: Dim, valor: string | null | undefined, ev?: MouseEvent | KeyboardEvent): void {
    if (valor === null || valor === undefined || valor === '') {
      return;
    }
    const actual = this.filtros[d] ?? [];
    const sumar = this.multiple || !!(ev && (ev.ctrlKey || ev.metaKey || ev.shiftKey));
    let nuevo: string[];
    if (sumar) {
      nuevo = actual.includes(valor) ? actual.filter((v) => v !== valor) : [...actual, valor];
    } else {
      nuevo = actual.length === 1 && actual[0] === valor ? [] : [valor];
    }
    this.ponerFiltro(d, nuevo);
  }

  private ponerFiltro(d: Dim, valores: string[]): void {
    const f = { ...this.filtros };
    if (valores.length) {
      f[d] = valores;
    } else {
      delete f[d];
    }
    this.filtros = f;
    this.alCambiar();
  }

  /** Celda de un mapa de calor: filtra por las dos dimensiones a la vez. */
  seleccionarCelda(a: Dim, va: string, b: Dim, vb: string, ev?: MouseEvent): void {
    const sumar = this.multiple || !!(ev && (ev.ctrlKey || ev.metaKey || ev.shiftKey));
    const yaEsta = this.activo(a, va) && this.activo(b, vb);
    const f = { ...this.filtros };
    if (sumar) {
      const ponQuita = (d: Dim, v: string) => {
        const act = f[d] ?? [];
        f[d] = yaEsta ? act.filter((x) => x !== v) : [...new Set([...act, v])];
        if (!f[d]!.length) delete f[d];
      };
      ponQuita(a, va);
      ponQuita(b, vb);
    } else if (yaEsta && f[a]?.length === 1 && f[b]?.length === 1) {
      delete f[a];
      delete f[b];
    } else {
      f[a] = [va];
      f[b] = [vb];
    }
    this.filtros = f;
    this.alCambiar();
  }

  quitarValor(d: Dim, v: string): void {
    this.ponerFiltro(d, (this.filtros[d] ?? []).filter((x) => x !== v));
  }

  quitar(d: Dim): void {
    this.ponerFiltro(d, []);
  }

  limpiar(): void {
    this.filtros = {};
    this.q = '';
    this.alCambiar();
  }

  activo(d: Dim, valor: string): boolean {
    return !!this.filtros[d]?.includes(valor);
  }

  atenuado(d: Dim, valor: string): boolean {
    return !!this.filtros[d]?.length && !this.filtros[d]!.includes(valor);
  }

  get chips(): { dim: Dim; etiqueta: string; valor: string; clave: string }[] {
    const r: { dim: Dim; etiqueta: string; valor: string; clave: string }[] = [];
    for (const d of DIMS) {
      for (const v of this.filtros[d] ?? []) {
        r.push({ dim: d, etiqueta: ETIQUETA_DIM[d], valor: this.textoFiltro(d, v), clave: v });
      }
    }
    return r;
  }

  get hayFiltros(): boolean {
    return Object.keys(this.filtros).length > 0 || !!this.q.trim();
  }

  get numFiltros(): number {
    return this.chips.length + (this.q.trim() ? 1 : 0);
  }

  textoFiltro(d: Dim, v: string): string {
    switch (d) {
      case 'etapa': return ETIQUETA_ETAPA_CORTA[v] ?? v;
      case 'mes': return this.etiquetaMes(v, true);
      case 'unidad': return this.nombreUnidad(v);
      case 'edad': return EDADES.find((e) => e.clave === v)?.etiqueta ?? v;
      case 'dia': return DIAS_SEMANA[Number(v)] ?? v;
      case 'hora': return `${v}:00–${this.finHora(v)}:00`;
      case 'origen': return v === 'Web' ? 'En línea' : v;
      default: return v;
    }
  }

  // ---- panel de filtros con listas

  alternarOpcion(d: Dim, v: string): void {
    const act = this.filtros[d] ?? [];
    this.ponerFiltro(d, act.includes(v) ? act.filter((x) => x !== v) : [...act, v]);
  }

  todasOpciones(d: Dim): void {
    this.ponerFiltro(d, (this.opciones[d] ?? []).map((o) => o.clave));
  }

  opcionesVisibles(d: Dim): OpcionFiltro[] {
    const t = this.normalizar(this.buscaOpcion[d] ?? '');
    const lista = this.opciones[d] ?? [];
    return t ? lista.filter((o) => this.normalizar(o.etiqueta).includes(t)) : lista;
  }

  cambiarTexto(): void {
    this.alCambiar();
  }

  /** Lo que se ejecuta tras cualquier cambio de filtros o periodo. */
  private alCambiar(): void {
    this.recalcular();
    this.escribirUrl();
  }

  // ====================================================================== URL y vistas guardadas

  private get estado(): EstadoPanel {
    const e: EstadoPanel = { filtros: this.filtros, dias: this.dias };
    if (this.desde) e.desde = this.desde;
    if (this.hasta) e.hasta = this.hasta;
    if (this.q.trim()) e.q = this.q.trim();
    return e;
  }

  private aplicarEstado(e: EstadoPanel, recalcular = true): void {
    this.filtros = { ...(e.filtros ?? {}) };
    this.dias = e.dias ?? 30;
    this.desde = e.desde ?? '';
    this.hasta = e.hasta ?? '';
    this.q = e.q ?? '';
    if (recalcular) {
      this.cambiarPeriodo();
    }
  }

  private estadoDesdeUrl(): EstadoPanel {
    const p = this.route.snapshot.queryParamMap;
    const filtros: Filtros = {};
    for (const d of DIMS) {
      const v = p.get(d);
      if (v) filtros[d] = v.split('|').filter(Boolean);
    }
    const dias = p.has('periodo') ? Number(p.get('periodo')) : 30;
    return {
      filtros,
      dias: Number.isFinite(dias) ? dias : 30,
      desde: p.get('desde') ?? undefined,
      hasta: p.get('hasta') ?? undefined,
      q: p.get('q') ?? undefined,
    };
  }

  /** Los filtros viajan en la dirección: se puede recargar, usar "atrás" o compartir el enlace. */
  private escribirUrl(): void {
    const qp: Record<string, string | null> = {};
    for (const d of DIMS) qp[d] = this.filtros[d]?.length ? this.filtros[d]!.join('|') : null;
    qp['periodo'] = this.dias === 30 ? null : String(this.dias);
    qp['desde'] = this.desde || null;
    qp['hasta'] = this.hasta || null;
    qp['q'] = this.q.trim() || null;
    this.router.navigate([], { relativeTo: this.route, queryParams: qp, replaceUrl: true });
  }

  copiarEnlace(): void {
    navigator.clipboard?.writeText(window.location.href).then(
      () => this.toast.exito('Enlace copiado: quien lo abra verá el panel con estos mismos filtros.'),
      () => this.toast.advertencia('No se pudo copiar el enlace.'),
    );
  }

  guardarVista(): void {
    const nombre = this.nombreVista.trim();
    if (!nombre) {
      this.toast.advertencia('Escribe un nombre para la vista.');
      return;
    }
    this.vistas = [...this.vistas.filter((v) => v.nombre !== nombre), { nombre, estado: JSON.parse(JSON.stringify(this.estado)) }];
    this.guardarLocal(CLAVE_VISTAS, this.vistas);
    this.nombreVista = '';
    this.toast.exito(`Vista «${nombre}» guardada.`);
  }

  abrirVista(v: VistaGuardada): void {
    this.aplicarEstado(JSON.parse(JSON.stringify(v.estado)));
    this.menuVistas = false;
  }

  borrarVista(v: VistaGuardada, ev: Event): void {
    ev.stopPropagation();
    this.vistas = this.vistas.filter((x) => x !== v);
    this.guardarLocal(CLAVE_VISTAS, this.vistas);
  }

  resumenVista(v: VistaGuardada): string {
    const partes = Object.entries(v.estado.filtros ?? {}).map(([d, vals]) => `${ETIQUETA_DIM[d as Dim]}: ${(vals as string[]).length}`);
    return partes.join(' · ') || 'Sin filtros';
  }

  // ====================================================================== personalizar

  get seccionesVisibles(): SeccionPanel[] {
    return this.personalizando ? this.orden : this.orden.filter((s) => !this.ocultas.has(s));
  }

  soltar(ev: CdkDragDrop<SeccionPanel[]>): void {
    moveItemInArray(this.orden, ev.previousIndex, ev.currentIndex);
    this.guardarDiseno();
  }

  mover(s: SeccionPanel, delta: number): void {
    const i = this.orden.indexOf(s);
    const j = i + delta;
    if (j < 0 || j >= this.orden.length) return;
    moveItemInArray(this.orden, i, j);
    this.guardarDiseno();
  }

  alternarOculta(s: SeccionPanel): void {
    if (this.ocultas.has(s)) this.ocultas.delete(s);
    else this.ocultas.add(s);
    this.guardarDiseno();
  }

  restablecerDiseno(): void {
    this.orden = [...ORDEN_INICIAL];
    this.ocultas.clear();
    this.guardarDiseno();
  }

  private guardarDiseno(): void {
    this.guardarLocal(CLAVE_DISENO, { orden: this.orden, ocultas: [...this.ocultas] });
  }

  private leerDiseno(): void {
    const d = this.leerLocal<{ orden?: SeccionPanel[]; ocultas?: SeccionPanel[] }>(CLAVE_DISENO, {});
    if (d.orden?.length) {
      // Conserva el orden guardado y agrega al final cualquier sección nueva.
      this.orden = [...d.orden.filter((s) => ORDEN_INICIAL.includes(s)), ...ORDEN_INICIAL.filter((s) => !d.orden!.includes(s))];
    }
    this.ocultas = new Set((d.ocultas ?? []).filter((s) => ORDEN_INICIAL.includes(s)));
  }

  // ====================================================================== ayuda

  alternarAyuda(id: string, ev?: Event): void {
    ev?.stopPropagation();
    this.ayudaAbierta = this.ayudaAbierta === id ? null : id;
  }

  textoAyuda(id: string): AyudaSeccion {
    return AYUDA[id];
  }

  iniciarRecorrido(): void {
    this.menuAyuda = false;
    this.personalizando = false;
    this.irAPaso(0);
  }

  irAPaso(i: number): void {
    if (i < 0) return;
    if (i >= PASOS_RECORRIDO.length) {
      this.terminarRecorrido();
      return;
    }
    this.paso = i;
    this.cdr.detectChanges();
    document.getElementById(PASOS_RECORRIDO[i].id)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  terminarRecorrido(): void {
    this.paso = null;
    this.guardarLocal(CLAVE_RECORRIDO, true);
  }

  enRecorrido(id: string): boolean {
    return this.paso !== null && PASOS_RECORRIDO[this.paso]?.id === id;
  }

  @HostListener('document:click')
  cerrarFlotantes(): void {
    this.ayudaAbierta = null;
    this.menuVistas = false;
    this.menuAyuda = false;
  }

  @HostListener('document:keydown.escape')
  alEscape(): void {
    this.cerrarFlotantes();
    this.panelFiltros = false;
    if (this.paso !== null) this.terminarRecorrido();
  }

  // ====================================================================== cálculo

  private recalcular(): void {
    const dia = 86400000;
    let ini = 0;
    let fin = Number.MAX_SAFE_INTEGER;
    if (this.dias === PERIODO_PERSONALIZADO) {
      ini = this.desde ? new Date(this.desde + 'T00:00:00').getTime() : 0;
      fin = this.hasta ? new Date(this.hasta + 'T23:59:59').getTime() : Date.now();
    } else if (this.dias > 0) {
      ini = Date.now() - this.dias * dia;
      fin = Date.now();
    }
    this.basePeriodo = ini || fin !== Number.MAX_SAFE_INTEGER ? this.todas.filter((f) => f.ms >= ini && f.ms <= fin) : this.todas;
    const largo = fin !== Number.MAX_SAFE_INTEGER && ini ? fin - ini : 0;
    this.periodoAnterior = largo ? this.todas.filter((f) => f.ms >= ini - largo && f.ms < ini) : [];

    this.vista = this.base();
    this.calcularEtapas();
    this.calcularMeses();
    this.calcularEdades();
    this.calcularCalorUnidades();
    this.calcularCalorDias();
    this.niveles = this.contar(this.base('nivel'), (f) => f.nivel);
    this.ramas = this.contar(this.base('rama'), (f) => f.rama).filter((r) => r.clave !== 'No aplica');
    this.origenes = this.contar(this.base('origen'), (f) => f.origen);
    this.prioridades = this.contar(this.base('prioridad'), (f) => f.prio);
    this.recepcionistas = this.contar(this.base('recepcionista'), (f) => f.recep).slice(0, 8);
    this.abogados = this.contar(this.base('abogado'), (f) => f.abog).slice(0, 8);
    this.antiguas = this.vista.filter((f) => f.abierta).sort((a, b) => b.dias - a.dias).slice(0, 8);
    this.calcularOpciones();
    this.calcularFicha();
    this.calcularHallazgos();
  }

  private contar(filas: Fila[], clave: (f: Fila) => string): Conteo[] {
    const m = new Map<string, Conteo>();
    for (const f of filas) {
      const k = clave(f);
      const c = m.get(k) ?? { clave: k, etiqueta: k, total: 0, abiertas: 0 };
      c.total++;
      if (f.abierta) c.abiertas++;
      m.set(k, c);
    }
    return [...m.values()].sort((a, b) => b.total - a.total);
  }

  /** Valores disponibles por dimensión, con cuántas quejas quedarían al elegirlos. */
  private calcularOpciones(): void {
    const ops: Partial<Record<Dim, OpcionFiltro[]>> = {};
    for (const d of DIMS) {
      const filas = this.base(d);
      const conteo = new Map<string, number>();
      for (const f of filas) {
        const v = this.valorDe(f, d);
        if (v) conteo.set(v, (conteo.get(v) ?? 0) + 1);
      }
      // Los valores seleccionados siempre aparecen, aunque ya no tengan quejas.
      for (const v of this.filtros[d] ?? []) if (!conteo.has(v)) conteo.set(v, 0);
      let lista = [...conteo.entries()].map(([clave, total]) => ({ clave, etiqueta: this.textoFiltro(d, clave), total }));
      const ordenNatural: Partial<Record<Dim, string[]>> = {
        etapa: ETAPAS, edad: [...EDADES.map((e) => e.clave), 'CERRADA'], dia: ['0', '1', '2', '3', '4', '5', '6'], hora: HORAS,
      };
      if (ordenNatural[d]) {
        const o = ordenNatural[d]!;
        lista = lista.sort((a, b) => o.indexOf(a.clave) - o.indexOf(b.clave));
      } else if (d === 'mes') {
        lista = lista.sort((a, b) => b.clave.localeCompare(a.clave));
      } else {
        lista = lista.sort((a, b) => b.total - a.total);
      }
      if (d === 'edad') lista = lista.map((o) => (o.clave === 'CERRADA' ? { ...o, etiqueta: 'Cerradas' } : o));
      ops[d] = lista;
    }
    this.opciones = ops;
  }

  private calcularEtapas(): void {
    const filas = this.base('etapa');
    this.porEtapa = ETAPAS.map((e) => {
      const deEtapa = filas.filter((f) => f.etapa === e);
      return { clave: e, total: deEtapa.length, estatus: this.contar(deEtapa, (f) => f.estatusEtiqueta).map((c) => ({ etiqueta: c.etiqueta, total: c.total })) };
    });
  }

  private calcularMeses(): void {
    // Siempre 12 meses hacia atrás: la tendencia se ve aunque el periodo sea corto.
    const filas = this.cumple(this.todas, ['mes']);
    const hoy = new Date();
    const claves: string[] = [];
    for (let i = 11; i >= 0; i--) {
      const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      claves.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
    }
    this.clavesMes = claves;
    this.etiquetasMes = claves.map((k) => this.etiquetaMes(k));
    this.seriesMes = ETAPAS.map((e) => ({
      nombre: ETIQUETA_ETAPA_CORTA[e],
      color: COLOR_ETAPA[e],
      valores: claves.map((k) => filas.filter((f) => f.mesClave === k && f.etapa === e).length),
    }));
  }

  private calcularEdades(): void {
    const filas = this.base('edad').filter((f) => f.abierta);
    this.seriesEdad = ETAPAS_ABIERTAS.map((e) => ({
      nombre: ETIQUETA_ETAPA_CORTA[e],
      color: COLOR_ETAPA[e],
      valores: EDADES.map((b) => filas.filter((f) => f.edadClave === b.clave && f.etapa === e).length),
    }));
  }

  private calcularCalorUnidades(): void {
    const filas = this.base('unidad', 'etapa');
    const previas = this.baseAnterior('unidad', 'etapa');
    const porUnidad = this.contar(filas, (f) => f.unidadClave ?? '').filter((u) => u.clave);
    const top = porUnidad.slice(0, TOP_UNIDADES);
    for (const sel of this.filtros.unidad ?? []) {
      if (!top.some((u) => u.clave === sel)) {
        const extra = porUnidad.find((u) => u.clave === sel);
        if (extra) top.push(extra);
      }
    }
    this.calorUnidades = top.map((u) => {
      const deUnidad = filas.filter((f) => f.unidadClave === u.clave);
      return {
        clave: u.clave,
        etiqueta: this.nombreUnidad(u.clave),
        total: u.total,
        anterior: previas.filter((f) => f.unidadClave === u.clave).length,
        celdas: ETAPAS.map((e) => ({ clave: e, valor: deUnidad.filter((f) => f.etapa === e).length })),
      };
    });
    this.maxCalorUnidad = Math.max(1, ...this.calorUnidades.flatMap((u) => u.celdas.map((c) => c.valor)));
  }

  private calcularCalorDias(): void {
    const filas = this.base('dia', 'hora');
    this.calorDias = DIAS_SEMANA.map((d, i) => {
      const delDia = filas.filter((f) => f.diaClave === String(i));
      return {
        clave: String(i),
        etiqueta: d,
        total: delDia.length,
        anterior: 0,
        celdas: HORAS.map((h) => ({ clave: h, valor: delDia.filter((f) => f.horaClave === h).length })),
      };
    });
    this.maxCalorDia = Math.max(1, ...this.calorDias.flatMap((d) => d.celdas.map((c) => c.valor)));
  }

  private mediana(valores: number[]): number {
    const d = [...valores].sort((a, b) => a - b);
    return d.length ? d[Math.floor(d.length / 2)] : 0;
  }

  private calcularFicha(): void {
    const sel = this.filtros.unidad ?? [];
    if (sel.length !== 1) {
      this.ficha = null;
      return;
    }
    const clave = sel[0];
    const filas = this.vista;
    const abiertas = filas.filter((f) => f.abierta);
    const ranking = this.contar(this.base('unidad'), (f) => f.unidadClave ?? '').filter((u) => u.clave);
    const totalPeriodo = this.base('unidad').length;
    this.ficha = {
      clave,
      nombre: this.nombreUnidad(clave),
      titular: titularDe(clave),
      total: filas.length,
      anterior: this.baseAnterior().length,
      abiertas: abiertas.length,
      pctPeriodo: totalPeriodo ? Math.round((filas.length / totalPeriodo) * 1000) / 10 : 0,
      lugar: ranking.findIndex((u) => u.clave === clave) + 1,
      deUnidades: ranking.length,
      mediana: this.mediana(abiertas.map((f) => f.dias)),
      medianaGeneral: this.mediana(this.base('unidad').filter((f) => f.abierta).map((f) => f.dias)),
      etapas: ETAPAS.map((e) => ({ clave: e, total: filas.filter((f) => f.etapa === e).length })),
    };
  }

  /** Frases automáticas con lo más relevante de la selección actual. */
  private calcularHallazgos(): void {
    const v = this.vista;
    const h: { texto: string; tipo: 'info' | 'alerta' | 'bien' }[] = [];
    if (!v.length) {
      this.hallazgos = [];
      return;
    }
    const abiertas = v.filter((f) => f.abierta);
    if (!this.filtros.unidad?.length) {
      const top = this.contar(v, (f) => f.unidadClave ?? '').filter((u) => u.clave)[0];
      if (top) h.push({ tipo: 'info', texto: `${this.nombreUnidad(top.clave)} concentra ${this.pct(top.total, v.length)} de las quejas de la selección (${top.total}).` });
    }
    // Unidades con alza fuerte contra el periodo anterior.
    const alzas = this.calorUnidades
      .filter((u) => u.anterior >= 3 && u.total >= u.anterior * 1.5)
      .sort((a, b) => b.total / Math.max(1, b.anterior) - a.total / Math.max(1, a.anterior));
    if (alzas.length) {
      const a = alzas[0];
      h.push({ tipo: 'alerta', texto: `${a.etiqueta} subió de ${a.anterior} a ${a.total} quejas contra el periodo anterior (+${variacion(a.total, a.anterior)}%).` });
    }
    const sinTomar = v.filter((f) => f.etapa === 'RECEPCION' && f.recep === SIN_RECEPCIONISTA).length;
    if (sinTomar && !this.filtros.etapa?.length) {
      h.push({ tipo: sinTomar > v.length * 0.3 ? 'alerta' : 'info', texto: `${this.pct(v.filter((f) => f.etapa === 'RECEPCION').length, v.length)} sigue en Recepción; ${sinTomar} nadie la ha tomado.` });
    }
    const viejas = abiertas.filter((f) => f.dias > 30).length;
    if (viejas) h.push({ tipo: 'alerta', texto: `${viejas} quejas abiertas llevan más de 30 días sin concluir.` });
    else if (abiertas.length) h.push({ tipo: 'bien', texto: 'Ninguna queja abierta de la selección supera los 30 días.' });
    const pico = this.calorDias.flatMap((d) => d.celdas.map((c) => ({ d: Number(d.clave), h: c.clave, v: c.valor }))).sort((a, b) => b.v - a.v)[0];
    if (pico && pico.v > 1 && !this.filtros.dia?.length && !this.filtros.hora?.length) {
      h.push({ tipo: 'info', texto: `Las quejas llegan sobre todo los ${DIAS_LARGOS[pico.d]} de ${pico.h}:00 a ${this.finHora(pico.h)}:00 (${pico.v}).` });
    }
    const anterior = this.baseAnterior().length;
    if (anterior) {
      const d = variacion(v.length, anterior) ?? 0;
      h.push({ tipo: d > 20 ? 'alerta' : d < 0 ? 'bien' : 'info', texto: `Llegaron ${v.length} quejas ${this.etiquetaPeriodo}: ${d >= 0 ? d + '% más' : -d + '% menos'} que el periodo anterior (${anterior}).` });
    }
    this.hallazgos = h.slice(0, 6);
  }

  // ====================================================================== indicadores

  get delta(): number | null {
    const ant = this.baseAnterior().length;
    return ant ? variacion(this.vista.length, ant) : null;
  }

  totalEtapa(e: string): number {
    return this.vista.filter((f) => f.etapa === e).length;
  }

  get sinTomarViejas(): number {
    return this.vista.filter((f) => f.etapa === 'RECEPCION' && f.recep === SIN_RECEPCIONISTA && f.dias > 3).length;
  }

  get abiertasVista(): number {
    return this.vista.filter((f) => f.abierta).length;
  }

  get medianaAbiertas(): number {
    return this.mediana(this.vista.filter((f) => f.abierta).map((f) => f.dias));
  }

  get totalEtapas(): number {
    return this.porEtapa.reduce((s, e) => s + e.total, 0);
  }

  // ====================================================================== apoyo de plantilla

  pct(n: number, total: number): string {
    if (!total) return '0%';
    const p = (n / total) * 100;
    return p > 0 && p < 1 ? '<1%' : `${Math.round(p)}%`;
  }

  tendencia(actual: number, anterior: number): { texto: string; clase: string } | null {
    if (!this.periodoAnterior.length) return null;
    if (!anterior) return actual ? { texto: 'nuevo', clase: 'sube' } : null;
    const d = variacion(actual, anterior) ?? 0;
    return { texto: `${d > 0 ? '↑' : d < 0 ? '↓' : '='}${Math.abs(d)}%`, clase: d > 15 ? 'sube' : d < -15 ? 'baja' : 'igual' };
  }

  ancho(n: number, lista: Conteo[]): number {
    return (n / Math.max(1, ...lista.map((c) => c.total))) * 100;
  }

  intensidad(valor: number, max: number): string {
    return valor ? `rgba(122, 31, 69, ${(0.12 + 0.88 * (valor / max)).toFixed(2)})` : 'transparent';
  }

  colorTexto(valor: number, max: number): string {
    return valor / max > 0.55 ? '#fff' : '#3b0a22';
  }

  etiquetaMes(clave: string, largo = false): string {
    const [a, m] = clave.split('-');
    const mes = MESES_CORTOS[Number(m) - 1] ?? m;
    return largo ? `${mes} ${a}` : `${mes} ${a.slice(2)}`;
  }

  private fechaCorta(iso: string): string {
    if (!iso) return '';
    const [a, m, d] = iso.split('-');
    return `${Number(d)} ${MESES_CORTOS[Number(m) - 1]?.toLowerCase()} ${a}`;
  }

  nombreUnidad(clave: string): string {
    if (!clave) return 'Sin unidad';
    return this.todas.find((f) => f.unidadClave === clave)?.unidadNombre || clave;
  }

  finHora(h: string): string {
    return String(Number(h) + 2).padStart(2, '0');
  }

  iniciales(nombre: string): string {
    const partes = (nombre || '').split(/\s+/).filter((p) => p && !p.endsWith('.') && p !== 'en');
    return ((partes[0]?.charAt(0) ?? '') + (partes[1]?.charAt(0) ?? '')).toUpperCase() || '?';
  }

  seleccionarMes(i: number): void {
    this.alternar('mes', this.clavesMes[i]);
  }

  seleccionarEdad(i: number): void {
    this.alternar('edad', EDADES[i]?.clave);
  }

  get mesesSeleccionados(): string {
    return (this.filtros.mes ?? []).map((m) => this.etiquetaMes(m, true)).join(', ');
  }

  get edadesSeleccionadas(): string {
    return (this.filtros.edad ?? []).map((e) => this.textoFiltro('edad', e)).join(', ');
  }

  mailto(t: TitularIpn, unidad: string): string {
    const asunto = encodeURIComponent(`Defensoría de los Derechos Politécnicos — Seguimiento de quejas de ${unidad}`);
    return `mailto:${t.correo}?subject=${asunto}`;
  }

  /** Abre el explorador de quejas con los filtros que este admite (uno por dimensión). */
  verEnQuejas(): void {
    const q: Record<string, string> = {};
    const uno = (d: Dim) => (this.filtros[d]?.length === 1 ? this.filtros[d]![0] : undefined);
    if (uno('etapa')) q['etapa'] = uno('etapa')!;
    if (uno('unidad')) q['unidad'] = uno('unidad')!;
    if (uno('mes')) q['mes'] = uno('mes')!;
    const prio = uno('prioridad');
    if (prio) q['prioridad'] = prio === SIN_PRIORIDAD ? 'SIN' : prio.toUpperCase();
    const abog = uno('abogado');
    if (abog) q['abogado'] = abog === SIN_ABOGADO ? 'SIN_ASIGNAR' : abog;
    if (uno('recepcionista') === SIN_RECEPCIONISTA) q['recepcionista'] = 'NADIE';
    if (this.q.trim()) q['q'] = this.q.trim();
    if (!q['mes']) {
      if (this.dias === PERIODO_PERSONALIZADO) {
        if (this.desde) q['desde'] = this.desde.slice(0, 7);
        if (this.hasta) q['hasta'] = this.hasta.slice(0, 7);
      } else if (this.dias > 0) {
        const d = new Date(Date.now() - this.dias * 86400000);
        q['desde'] = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      }
    }
    this.router.navigate(['/quejas'], { queryParams: q });
  }

  exportar(): void {
    const cab = ['Folio', 'Fecha', 'Quejoso', 'Unidad', 'Nivel', 'Rama', 'Etapa', 'Estatus', 'Días', 'Recepcionista', 'Abogado', 'Origen', 'Prioridad'];
    const esc = (s: unknown) => `"${String(s ?? '').replace(/"/g, '""')}"`;
    const filas = this.vista.map((f) => [f.folio, f.fecha?.slice(0, 16).replace('T', ' '), f.quejoso, f.unidadNombre, f.nivel, f.rama,
      ETIQUETA_ETAPA_CORTA[f.etapa] ?? f.etapa, f.estatusEtiqueta, f.dias, f.recep, f.abog, f.origen, f.prio].map(esc).join(','));
    const csv = '﻿' + [cab.map(esc).join(','), ...filas].join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `quejas-seleccion-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  abrir(v: Ventana): void {
    this.ventana = v;
  }

  cerrar(): void {
    this.ventana = null;
  }

  get maxMotivo(): number {
    return Math.max(1, ...(this.panel?.motivos ?? []).map((m) => m.cantidad));
  }

  detener(ev: Event): void {
    ev.stopPropagation();
  }

  private normalizar(s: string): string {
    return (s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }

  private leerLocal<T>(clave: string, defecto: T): T {
    try {
      const v = localStorage.getItem(clave);
      return v ? (JSON.parse(v) as T) : defecto;
    } catch {
      return defecto;
    }
  }

  private guardarLocal(clave: string, valor: unknown): void {
    try {
      localStorage.setItem(clave, JSON.stringify(valor));
    } catch {
      /* sin almacenamiento: solo dura esta sesión */
    }
  }
}
