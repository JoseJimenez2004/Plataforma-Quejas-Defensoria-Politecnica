import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Dependencia } from '../../core/models/catalogo.models';

/** Una dependencia con sus campos ya normalizados, para no repetir el trabajo en cada tecla. */
interface EntradaIndice {
  dep: Dependencia;
  nombreNorm: string;
  claveNorm: string;
  abrevNorm: string;
  /** Palabras del nombre, normalizadas — permiten buscar "escuela superior co". */
  palabras: string[];
}

/** Una sugerencia lista para pintar, con el tramo del nombre que coincidió. */
interface Sugerencia {
  dep: Dependencia;
  puntaje: number;
  antes: string;
  coincide: string;
  despues: string;
}

/** Máximo de sugerencias visibles. Más que esto deja de ser una lista y vuelve a ser el
 * problema que estamos resolviendo. */
const MAXIMO_SUGERENCIAS = 8;

/**
 * Tipo de dependencia que se prefiere a igualdad de puntaje.
 *
 * El campo pregunta DÓNDE ocurrieron los hechos, y de las 209 dependencias solo 45 son
 * escuelas — pero son el destino de la enorme mayoría de las quejas. Sin este desempate,
 * escribir "computo" pone "División de Cómputo" (un área administrativa) por encima de
 * "Escuela Superior de Cómputo", nada más porque su nombre es más corto.
 */
const TIPO_PREFERIDO = 'Unidad Académica';

/**
 * Buscador con autocompletado para el catálogo de dependencias del IPN.
 *
 * Reemplaza al `<select>` de 209 opciones: escribiendo "ESCOM", "escom" o
 * "escuela superior co" aparecen las coincidencias y se elige una.
 *
 * DECISIÓN: el filtrado es **en el cliente**, no en el servidor. El catálogo completo ya se
 * descarga una sola vez al abrir la pantalla (unos 40 KB), así que buscar en memoria responde
 * al instante, funciona aunque la red se ponga lenta y no le pega al backend una vez por cada
 * tecla. Con 209 registros no hay ninguna ganancia en mover esto al servidor; con decenas de
 * miles sí la habría, y entonces el cambio sería agregar un endpoint de búsqueda y un debounce
 * aquí — el resto del componente no cambiaría.
 *
 * El valor que expone al formulario es la **clave** de la dependencia (p. ej. "ESCOM"), igual
 * que hacía el `<select>`, así que el backend y la base no se enteran del cambio.
 */
@Component({
  selector: 'app-autocompletar-dependencia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './autocompletar-dependencia.html',
  styleUrl: './autocompletar-dependencia.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompletarDependencia),
      multi: true,
    },
  ],
})
export class AutocompletarDependencia implements ControlValueAccessor, OnChanges {
  @Input() dependencias: Dependencia[] = [];
  @Input() cargando = false;
  @Input() placeholder = 'Escribe el nombre o las siglas (ej. ESCOM)';
  /** Se emite además del ngModelChange, por si el padre necesita reaccionar a la selección. */
  @Output() cambio = new EventEmitter<string>();

  /** Lo que el usuario ve escrito en la caja. */
  texto = '';
  /** La clave realmente seleccionada. Vacía mientras no se elija de la lista. */
  valor = '';
  abierto = false;
  disabled = false;
  /** Índice de la sugerencia resaltada con el teclado; -1 = ninguna. */
  resaltado = -1;
  sugerencias: Sugerencia[] = [];
  /** Cuántas coincidencias quedaron fuera del tope visible. */
  ocultas = 0;

  private indice: EntradaIndice[] = [];
  /** Clave que llegó por writeValue antes de que el catálogo estuviera cargado. */
  private clavePendiente = '';

  private onChange: (val: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  ngOnChanges(cambios: SimpleChanges): void {
    if (cambios['dependencias']) {
      this.construirIndice();
      // Si el formulario ya traía una clave (p. ej. al editar una queja) y el catálogo acaba
      // de llegar, recién ahora se puede mostrar su nombre.
      if (this.clavePendiente) {
        this.mostrarClave(this.clavePendiente);
        this.clavePendiente = '';
      }
    }
  }

  // =======================================================================================
  // ControlValueAccessor
  // =======================================================================================

  writeValue(valor: string | null): void {
    const clave = valor ?? '';
    if (!clave) {
      this.valor = '';
      this.texto = '';
      return;
    }
    if (this.indice.length === 0) {
      this.clavePendiente = clave;
      this.valor = clave;
      return;
    }
    this.mostrarClave(clave);
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

  // =======================================================================================
  // Índice y normalización
  // =======================================================================================

  /**
   * Quita acentos y pasa a minúsculas SIN cambiar la longitud del texto.
   *
   * El truco de `[...s].map(c => c.normalize('NFD')[0])` es a propósito: descomponer la cadena
   * completa con NFD la alarga (una "ó" se vuelve "o" + tilde combinante), y entonces las
   * posiciones dejarían de coincidir con el texto original, que es justo lo que se necesita
   * para resaltar el tramo que coincidió. Normalizando carácter por carácter y quedándose con
   * el primero, cada letra sigue ocupando una posición.
   */
  private normalizar(texto: string): string {
    return [...texto]
      .map((caracter) => caracter.normalize('NFD')[0])
      .join('')
      .toLowerCase();
  }

  private construirIndice(): void {
    this.indice = (this.dependencias ?? []).map((dep) => {
      const nombreNorm = this.normalizar(dep.nombre ?? '');
      return {
        dep,
        nombreNorm,
        claveNorm: this.normalizar(dep.clave ?? ''),
        abrevNorm: this.normalizar(dep.abreviatura ?? ''),
        palabras: nombreNorm.split(/[^a-z0-9]+/).filter((p) => p.length > 0),
      };
    });
  }

  private mostrarClave(clave: string): void {
    const encontrada = this.indice.find((e) => e.dep.clave === clave);
    this.valor = clave;
    this.texto = encontrada ? this.etiqueta(encontrada.dep) : clave;
  }

  etiqueta(dep: Dependencia): string {
    return dep.nombre;
  }

  // =======================================================================================
  // Búsqueda
  // =======================================================================================

  /**
   * Puntúa qué tan buena es una coincidencia. Los números no significan nada por sí solos;
   * lo que importa es el orden: primero las siglas exactas, luego los prefijos, y hasta el
   * final las coincidencias en medio del texto.
   *
   * Sin esta escala, escribir "ESCOM" pondría igual de arriba a cualquier escuela cuyo nombre
   * contenga "com" — Comercio, Computación, etc. — y la que el usuario quiere quedaría
   * enterrada.
   */
  private puntuar(entrada: EntradaIndice, consulta: string, tokens: string[]): number {
    if (entrada.claveNorm === consulta || entrada.abrevNorm === consulta) return 100;
    if (entrada.claveNorm.startsWith(consulta) || entrada.abrevNorm.startsWith(consulta)) return 90;
    if (entrada.nombreNorm.startsWith(consulta)) return 80;
    // Cada palabra escrita debe ser el principio de alguna palabra del nombre. Esto es lo que
    // hace que "escuela superior co" encuentre "Escuela Superior de Cómputo".
    if (tokens.length > 1 && tokens.every((t) => entrada.palabras.some((p) => p.startsWith(t)))) {
      return 70;
    }
    if (entrada.nombreNorm.includes(consulta)) return 60;
    if (entrada.claveNorm.includes(consulta) || entrada.abrevNorm.includes(consulta)) return 50;
    return 0;
  }

  private buscar(consultaCruda: string): void {
    const consulta = this.normalizar(consultaCruda.trim());

    if (!consulta) {
      // Con la caja vacía se muestran las primeras opciones, para que abrir el campo sin
      // escribir nada siga siendo útil.
      this.sugerencias = this.indice
        .slice(0, MAXIMO_SUGERENCIAS)
        .map((e) => ({ dep: e.dep, puntaje: 0, antes: e.dep.nombre, coincide: '', despues: '' }));
      this.ocultas = Math.max(0, this.indice.length - MAXIMO_SUGERENCIAS);
      this.resaltado = -1;
      return;
    }

    const tokens = consulta.split(/\s+/).filter((t) => t.length > 0);

    const coincidencias = this.indice
      .map((entrada) => ({ entrada, puntaje: this.puntuar(entrada, consulta, tokens) }))
      .filter((c) => c.puntaje > 0)
      .sort((a, b) => {
        if (b.puntaje !== a.puntaje) return b.puntaje - a.puntaje;

        // A igual puntaje, primero las unidades académicas (ver TIPO_PREFERIDO).
        const preferidoA = a.entrada.dep.tipo === TIPO_PREFERIDO ? 0 : 1;
        const preferidoB = b.entrada.dep.tipo === TIPO_PREFERIDO ? 0 : 1;
        if (preferidoA !== preferidoB) return preferidoA - preferidoB;

        // Y después el nombre más corto: "Escuela Superior de Cómputo" antes que
        // "Escuela Superior de Comercio y Administración, Unidad Santo Tomás".
        const largo = a.entrada.dep.nombre.length - b.entrada.dep.nombre.length;
        return largo !== 0 ? largo : a.entrada.dep.nombre.localeCompare(b.entrada.dep.nombre, 'es');
      });

    this.ocultas = Math.max(0, coincidencias.length - MAXIMO_SUGERENCIAS);
    this.sugerencias = coincidencias
      .slice(0, MAXIMO_SUGERENCIAS)
      .map((c) => this.conResaltado(c.entrada, consulta, c.puntaje));
    this.resaltado = this.sugerencias.length > 0 ? 0 : -1;
  }

  /** Parte el nombre en tres para poder subrayar el tramo que coincidió, sin usar innerHTML. */
  private conResaltado(entrada: EntradaIndice, consulta: string, puntaje: number): Sugerencia {
    const nombre = entrada.dep.nombre;
    const desde = entrada.nombreNorm.indexOf(consulta);

    if (desde < 0) {
      return { dep: entrada.dep, puntaje, antes: nombre, coincide: '', despues: '' };
    }
    return {
      dep: entrada.dep,
      puntaje,
      antes: nombre.slice(0, desde),
      coincide: nombre.slice(desde, desde + consulta.length),
      despues: nombre.slice(desde + consulta.length),
    };
  }

  // =======================================================================================
  // Interacción
  // =======================================================================================

  onEntrada(valorEscrito: string): void {
    this.texto = valorEscrito;
    // Mientras no se elija de la lista, el formulario no tiene ninguna dependencia válida.
    // Es lo que impide enviar una queja con un lugar de los hechos que el usuario tecleó a
    // mano y no existe en el catálogo.
    if (this.valor) {
      this.valor = '';
      this.onChange('');
      this.cambio.emit('');
    }
    this.buscar(valorEscrito);
    this.abierto = true;
  }

  onFoco(): void {
    if (this.disabled) return;
    this.buscar(this.valor ? '' : this.texto);
    this.abierto = true;
  }

  seleccionar(dep: Dependencia): void {
    this.valor = dep.clave;
    this.texto = this.etiqueta(dep);
    this.abierto = false;
    this.resaltado = -1;
    this.onChange(dep.clave);
    this.onTouched();
    this.cambio.emit(dep.clave);
  }

  limpiar(): void {
    this.texto = '';
    this.valor = '';
    this.abierto = false;
    this.onChange('');
    this.cambio.emit('');
    this.enfocarCaja();
  }

  onTecla(evento: KeyboardEvent): void {
    if (this.disabled) return;

    switch (evento.key) {
      case 'ArrowDown':
        evento.preventDefault();
        if (!this.abierto) {
          this.onFoco();
          return;
        }
        this.mover(1);
        return;

      case 'ArrowUp':
        evento.preventDefault();
        this.mover(-1);
        return;

      case 'Enter':
        // preventDefault siempre que la lista esté abierta: este campo vive dentro de un
        // <form>, y sin esto un Enter para elegir una opción enviaría la queja completa.
        if (this.abierto && this.resaltado >= 0 && this.sugerencias[this.resaltado]) {
          evento.preventDefault();
          this.seleccionar(this.sugerencias[this.resaltado].dep);
        }
        return;

      case 'Escape':
        if (this.abierto) {
          evento.preventDefault();
          this.abierto = false;
          this.resaltado = -1;
        }
        return;

      case 'Tab':
        this.abierto = false;
        this.onTouched();
        return;

      default:
        return;
    }
  }

  private mover(paso: number): void {
    if (this.sugerencias.length === 0) return;
    const total = this.sugerencias.length;
    this.resaltado = (this.resaltado + paso + total) % total;
    this.desplazarAlResaltado();
  }

  /** Mantiene visible la opción resaltada cuando se navega con las flechas. */
  private desplazarAlResaltado(): void {
    const opcion = this.elementRef.nativeElement.querySelector<HTMLElement>(
      `#opcion-${this.resaltado}`,
    );
    opcion?.scrollIntoView({ block: 'nearest' });
  }

  private enfocarCaja(): void {
    this.elementRef.nativeElement.querySelector<HTMLInputElement>('input')?.focus();
  }

  /** Cerrar al hacer clic fuera — mismo patrón que el datepicker. */
  @HostListener('document:click', ['$event'])
  alClicarFuera(evento: MouseEvent): void {
    if (!this.abierto) return;
    if (!this.elementRef.nativeElement.contains(evento.target as Node)) {
      this.abierto = false;
      this.resaltado = -1;
      this.onTouched();
    }
  }

  /** true cuando hay texto escrito pero ninguna opción elegida todavía. */
  get sinElegir(): boolean {
    return this.texto.trim().length > 0 && this.valor === '';
  }
}
