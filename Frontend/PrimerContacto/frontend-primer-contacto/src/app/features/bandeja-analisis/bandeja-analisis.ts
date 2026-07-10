import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
import { ExpedienteBandeja } from '../../core/models/expediente-bandeja';
import { BandejaService } from '../../core/services/bandeja.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

type CampoFacetado = 'prioridades' | 'temas' | 'escuelas' | 'estatus';

interface FiltrosBandeja {
  texto: string;
  orden: 'recientes' | 'antiguos';
  prioridades: string[];
  temas: string[];
  escuelas: string[];
  estatus: string[];
}

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
    MatSnackBarModule
  ],
  templateUrl: './bandeja-analisis.html',
  styleUrl: './bandeja-analisis.css'
})
export class BandejaAnalisis implements OnInit {

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

  // Copia sin filtrar tal como llega del backend; los filtros siempre
  // se recalculan a partir de esta lista, nunca se pierde el original.
  private expedientesOriginal: ExpedienteBandeja[] = [];

  prioridades: string[] = ['Alta', 'Media', 'Baja'];

  temas: string[] = ['Género', 'Académico', 'Inclusión'];

  // TODO: catálogo temporal mientras se define el catálogo oficial de
  // unidades académicas del IPN. Reemplazar cuando exista el servicio real.
  escuelas: string[] = [
    'ESCOM',
    'ESIME Zacatenco',
    'ESIME Culhuacán',
    'ESIQIE',
    'ESFM',
    'ENCB',
    'UPIICSA',
    'ESCA Santo Tomás',
    'ESCA Tepepan',
    'ESIA Zacatenco'
  ];

  filtros: FiltrosBandeja = {
    texto: '',
    orden: 'recientes',
    prioridades: [],
    temas: [],
    escuelas: [],
    estatus: []
  };

  ngOnInit(): void {
    this.cargarBandeja();
  }

  cargarBandeja(): void {
    this.bandejaService.obtenerBandeja().subscribe({
      next: (expedientes) => {
        this.expedientesOriginal = expedientes;
        this.aplicarFiltros();
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

  // Los estatus no son un catálogo fijo definido en el frontend (vienen
  // del backend y pueden crecer), así que la lista de opciones del filtro
  // se arma a partir de lo que realmente llegó, no de una lista inventada.
  get estatusDisponibles(): string[] {
    return Array.from(new Set(this.expedientesOriginal.map(e => e.estatus))).sort();
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
    const texto = this.filtros.texto.trim().toLowerCase();

    let resultado = this.expedientesOriginal.filter(expediente => {
      const coincideTexto =
        !texto ||
        expediente.folio.toLowerCase().includes(texto) ||
        expediente.nombreQuejoso.toLowerCase().includes(texto);

      const coincidePrioridad =
        this.filtros.prioridades.length === 0 ||
        this.filtros.prioridades.includes(expediente.prioridad);

      const coincideTema =
        this.filtros.temas.length === 0 || this.filtros.temas.includes(expediente.tema);

      const coincideEscuela =
        this.filtros.escuelas.length === 0 ||
        this.filtros.escuelas.includes(expediente.unidadAcademica);

      const coincideEstatus =
        this.filtros.estatus.length === 0 || this.filtros.estatus.includes(expediente.estatus);

      return (
        coincideTexto &&
        coincidePrioridad &&
        coincideTema &&
        coincideEscuela &&
        coincideEstatus
      );
    });

    resultado = resultado.sort((a, b) => {
      const fechaA = this.convertirFechaATimestamp(a.fechaRecepcion);
      const fechaB = this.convertirFechaATimestamp(b.fechaRecepcion);

      return this.filtros.orden === 'recientes' ? fechaB - fechaA : fechaA - fechaB;
    });

    this.expedientes.data = resultado;
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

  private convertirFechaATimestamp(fecha: string): number {
    if (!fecha) return 0;

    const [dia, mes, anio] = fecha.split('/');
    return new Date(Number(anio), Number(mes) - 1, Number(dia)).getTime();
  }

  analizar(expediente: ExpedienteBandeja): void {
    this.router.navigate(
      ['/expediente', expediente.folio],
      {
        state: {
          quejaId: expediente.quejaId
        }
      }
    );
  }
}