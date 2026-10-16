import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription, debounceTime, switchMap } from 'rxjs';
import { ExpedienteBandeja, FiltroBandeja } from '../../core/models/expediente-bandeja';
import { BandejaService } from '../../core/services/bandeja.service';
import { claseEstatus, etiquetaEstatus } from '../../core/utils/estatus-expediente';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

type CampoFacetado = 'prioridades' | 'temas' | 'escuelas' | 'estatus';

interface FiltrosBandeja {
  texto: string;
  orden: 'recientes' | 'antiguos';
  prioridades: string[];
  temas: string[];
  escuelas: string[];
  /** Códigos del backend (EN_ANALISIS...), no etiquetas. */
  estatus: string[];
}

/** Orden del diagrama de estados, para listar la faceta de estatus. */
const ORDEN_ESTATUS = ['TURNADA', 'EN_ANALISIS', 'PROCEDENTE', 'IMPROCEDENTE', 'REMITIDA'];

@Component({
  selector: 'app-bandeja-analisis',
  imports: [
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './bandeja-analisis.html',
  styleUrl: './bandeja-analisis.css'
})
export class BandejaAnalisis implements OnInit, OnDestroy {

  constructor(
    private router: Router,
    private bandejaService: BandejaService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  displayedColumns = [
    'folio',
    'fechaRecepcion',
    'nombreQuejoso',
    'unidadAcademica',
    'tema',
    'prioridad',
    'estatus',
    'acciones'
  ];

  expedientes = new MatTableDataSource<ExpedienteBandeja>([]);

  /*
   * Lista completa, solo para armar las opciones de cada faceta y sus
   * conteos. Los RESULTADOS los filtra el backend (CU-PC-02).
   */
  private expedientesOriginal: ExpedienteBandeja[] = [];

  prioridades: string[] = ['Alta', 'Media', 'Baja'];

  readonly etiquetaEstatus = etiquetaEstatus;
  readonly claseEstatus = claseEstatus;

  filtros: FiltrosBandeja = {
    texto: '',
    orden: 'recientes',
    prioridades: [],
    temas: [],
    escuelas: [],
    estatus: []
  };

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
          this.expedientes.data = resultado;
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
    this.bandejaService.obtenerBandeja().subscribe({
      next: (expedientes) => {
        this.expedientesOriginal = expedientes;
        this.expedientes.data = expedientes;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open(
          'No fue posible cargar la bandeja de análisis.',
          'Cerrar',
          {
            duration: 3000
          }
        );
      }
    });
  }

  // Las opciones salen de los expedientes que realmente llegaron, no de
  // catálogos escritos a mano (antes temas y escuelas eran listas fijas).
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

  get totalFiltrosActivos(): number {
    return (
      this.filtros.prioridades.length +
      this.filtros.temas.length +
      this.filtros.escuelas.length +
      this.filtros.estatus.length
    );
  }

  estaSeleccionado(campo: CampoFacetado, valor: string): boolean {
    return this.filtros[campo].includes(valor);
  }

  toggleValor(campo: CampoFacetado, valor: string): void {
    const lista = this.filtros[campo];
    const indice = lista.indexOf(valor);

    if (indice === -1) {
      lista.push(valor);
    } else {
      lista.splice(indice, 1);
    }

    this.aplicarFiltros();
  }

  contarCoincidencias(campo: keyof ExpedienteBandeja, valor: string): number {
    return this.expedientesOriginal.filter(expediente => expediente[campo] === valor).length;
  }

  aplicarFiltros(): void {
    this.cambios$.next();
  }

  limpiarFiltros(): void {
    this.filtros = {
      texto: '',
      orden: 'recientes',
      prioridades: [],
      temas: [],
      escuelas: [],
      estatus: []
    };

    this.aplicarFiltros();
  }

  analizar(expediente: ExpedienteBandeja): void {
    this.router.navigate([
      '/expediente',
      expediente.folio
    ]);
  }

  private construirFiltro(): FiltroBandeja {
    return {
      texto: this.filtros.texto.trim() || undefined,
      prioridades: this.filtros.prioridades.map(p => p.toUpperCase()),
      estatusLista: this.filtros.estatus,
      unidadesAcademicas: this.filtros.escuelas,
      temas: this.filtros.temas,
      orden: this.filtros.orden
    };
  }

  private valoresDe(campo: 'tema' | 'unidadAcademica'): string[] {
    return Array.from(new Set(this.expedientesOriginal.map(e => e[campo]).filter(Boolean)))
      .sort((a, b) => a.localeCompare(b, 'es'));
  }
}
