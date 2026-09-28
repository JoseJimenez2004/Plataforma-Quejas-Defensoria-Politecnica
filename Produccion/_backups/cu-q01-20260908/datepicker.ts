import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

interface CeldaDia {
  dia: number;
  iso: string;
  fueraDeMes: boolean;
  deshabilitado: boolean;
  esHoy: boolean;
  esSeleccionado: boolean;
}

const NOMBRES_MES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];
const NOMBRES_DIA = ['D', 'L', 'M', 'M', 'J', 'V', 'S'];

/** Selector de fecha propio (reemplaza el <input type="date"> nativo). Se necesitaba porque
 * el picker nativo se ve distinto/feo entre navegadores y, en varios celulares, la rueda de
 * año se traba al tener que desplazarse manualmente año por año desde hoy hasta, por ejemplo,
 * 2005 para una fecha de nacimiento. Aquí la navegación de año/mes es con selects directos. */
@Component({
  selector: 'app-datepicker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './datepicker.html',
  styleUrl: './datepicker.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Datepicker),
      multi: true,
    },
  ],
})
export class Datepicker implements ControlValueAccessor {
  @Input() placeholder = 'dd/mm/aaaa';
  /** Fecha máxima seleccionable, formato ISO yyyy-mm-dd. */
  @Input() max = '';
  /** Fecha mínima seleccionable, formato ISO yyyy-mm-dd. Si no se da, se usa 100 años atrás. */
  @Input() min = '';
  /** Se emite además del ngModelChange normal, para poder enganchar lógica extra (p. ej.
   * detectar menor de edad) igual que el (change) del input nativo. */
  @Output() cambio = new EventEmitter<string>();

  readonly nombresMes = NOMBRES_MES;
  readonly nombresDia = NOMBRES_DIA;

  valor = '';
  abierto = false;
  disabled = false;

  vistaAno = new Date().getFullYear();
  vistaMes = new Date().getMonth();

  private onChange: (val: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  @HostListener('document:click', ['$event'])
  onDocClick(event: MouseEvent): void {
    if (this.abierto && !this.elementRef.nativeElement.contains(event.target as Node)) {
      this.abierto = false;
    }
  }

  writeValue(val: string): void {
    this.valor = val || '';
    const base = this.valor || this.max || this.hoyIso();
    const [ano, mes] = base.split('-').map(Number);
    this.vistaAno = ano;
    this.vistaMes = mes - 1;
  }

  registerOnChange(fn: (val: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }

  get textoMostrado(): string {
    if (!this.valor) return '';
    const [ano, mes, dia] = this.valor.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  get anoMinimo(): number {
    return this.min ? Number(this.min.split('-')[0]) : new Date().getFullYear() - 100;
  }

  get anoMaximo(): number {
    return this.max ? Number(this.max.split('-')[0]) : new Date().getFullYear() + 1;
  }

  get listaAnos(): number[] {
    const anos: number[] = [];
    for (let a = this.anoMaximo; a >= this.anoMinimo; a--) anos.push(a);
    return anos;
  }

  alternar(): void {
    if (this.disabled) return;
    this.abierto = !this.abierto;
    if (this.abierto) this.onTouched();
  }

  cerrar(): void {
    this.abierto = false;
  }

  cambiarMesVista(delta: number): void {
    let mes = this.vistaMes + delta;
    let ano = this.vistaAno;
    if (mes < 0) {
      mes = 11;
      ano--;
    } else if (mes > 11) {
      mes = 0;
      ano++;
    }
    this.vistaMes = mes;
    this.vistaAno = ano;
  }

  seleccionarDia(celda: CeldaDia): void {
    if (celda.deshabilitado) return;
    this.valor = celda.iso;
    this.onChange(this.valor);
    this.cambio.emit(this.valor);
    this.abierto = false;
    // Si el día pertenece a otro mes (celda de relleno), movemos la vista para que al volver
    // a abrir el calendario ya se vea el mes correcto.
    const [ano, mes] = celda.iso.split('-').map(Number);
    this.vistaAno = ano;
    this.vistaMes = mes - 1;
  }

  irAHoy(): void {
    const hoy = this.hoyIso();
    if (this.max && hoy > this.max) return;
    if (this.min && hoy < this.min) return;
    const [ano, mes] = hoy.split('-').map(Number);
    this.vistaAno = ano;
    this.vistaMes = mes - 1;
    this.valor = hoy;
    this.onChange(this.valor);
    this.cambio.emit(this.valor);
    this.abierto = false;
  }

  limpiar(): void {
    this.valor = '';
    this.onChange('');
    this.cambio.emit('');
    this.abierto = false;
  }

  get semanas(): CeldaDia[][] {
    const primerDiaMes = new Date(this.vistaAno, this.vistaMes, 1);
    const diasEnMes = new Date(this.vistaAno, this.vistaMes + 1, 0).getDate();
    const diasEnMesAnterior = new Date(this.vistaAno, this.vistaMes, 0).getDate();
    const inicioSemana = primerDiaMes.getDay(); // 0=domingo

    const celdas: CeldaDia[] = [];

    // Relleno con días del mes anterior
    for (let i = inicioSemana - 1; i >= 0; i--) {
      const dia = diasEnMesAnterior - i;
      celdas.push(this.crearCelda(dia, this.vistaMes - 1, this.vistaAno, true));
    }
    // Días del mes actual
    for (let dia = 1; dia <= diasEnMes; dia++) {
      celdas.push(this.crearCelda(dia, this.vistaMes, this.vistaAno, false));
    }
    // Relleno con días del mes siguiente hasta completar semanas de 7
    let siguiente = 1;
    while (celdas.length % 7 !== 0) {
      celdas.push(this.crearCelda(siguiente++, this.vistaMes + 1, this.vistaAno, true));
    }

    const semanas: CeldaDia[][] = [];
    for (let i = 0; i < celdas.length; i += 7) {
      semanas.push(celdas.slice(i, i + 7));
    }
    return semanas;
  }

  private crearCelda(dia: number, mes: number, ano: number, fueraDeMes: boolean): CeldaDia {
    let m = mes;
    let a = ano;
    if (m < 0) {
      m = 11;
      a--;
    } else if (m > 11) {
      m = 0;
      a++;
    }
    const iso = `${a}-${String(m + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
    return {
      dia,
      iso,
      fueraDeMes,
      deshabilitado: (!!this.max && iso > this.max) || (!!this.min && iso < this.min),
      esHoy: iso === this.hoyIso(),
      esSeleccionado: iso === this.valor,
    };
  }

  private hoyIso(): string {
    return new Date().toISOString().split('T')[0];
  }
}
