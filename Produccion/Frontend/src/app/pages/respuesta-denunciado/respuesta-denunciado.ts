import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CatalogoService } from '../../core/services/catalogo.service';
import { DenunciadoService } from '../../core/services/denunciado.service';
import { ToastService } from '../../core/services/toast.service';
import { Dependencia } from '../../core/models/catalogo.models';
import { RespuestaRegistrada } from '../../core/models/denunciado.models';
import { AutocompletarDependencia } from '../../shared/autocompletar-dependencia/autocompletar-dependencia';
import * as Reglas from '../../core/validaciones/reglas-queja';

/**
 * TEMPORAL: folio fijo de la queja a la que responde el denunciado. Más adelante llegará por
 * la URL (enlace que se le envíe) o por consulta; solo hay que cambiar esta constante por
 * la lectura de ActivatedRoute.
 */
export const FOLIO_QUEJA_PRUEBA = 'FOL-DEMO0001';

/** Versión del aviso de privacidad del denunciado (texto propio, distinto al del quejoso). */
export const AVISO_DENUNCIADO_VERSION = 'D-1.0';

/** Máximo de evidencias que acepta denunciado-service. */
const EVIDENCIA_CANTIDAD_MAXIMA = 10;

interface ArchivoSeleccionado {
  archivo: File;
  vistaPrevia: string | null;
  extension: string;
}

type TipoIdentificacion = 'ALUMNO' | 'EMPLEADO';

@Component({
  selector: 'app-respuesta-denunciado',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AutocompletarDependencia],
  templateUrl: './respuesta-denunciado.html',
  styleUrl: './respuesta-denunciado.scss',
})
export class RespuestaDenunciado implements OnInit, OnDestroy {
  readonly folioQueja = FOLIO_QUEJA_PRUEBA;
  readonly nombreLongitudMaxima = Reglas.NOMBRE_LONGITUD_MAXIMA;
  readonly descripcionMaxima = Reglas.DESCRIPCION_LONGITUD_MAXIMA;
  readonly formatearTamanio = Reglas.formatearTamanio;

  nombre = '';
  apellido1 = '';
  apellido2 = '';
  unidadProcedenciaClave = '';
  tipoIdentificacion: TipoIdentificacion = 'ALUMNO';
  numeroIdentificacion = '';
  descripcionHechos = '';
  avisoAceptado = false;
  mostrarAvisoCompleto = false;

  credencial: ArchivoSeleccionado[] = [];
  evidencias: ArchivoSeleccionado[] = [];

  dependencias: Dependencia[] = [];
  cargandoDependencias = true;

  intentoEnviar = false;
  enviando = false;
  errorEnvio = '';
  registrada: RespuestaRegistrada | null = null;

  constructor(
    private catalogoService: CatalogoService,
    private denunciadoService: DenunciadoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.catalogoService.listarDependencias().subscribe({
      next: (lista) => {
        this.dependencias = lista;
        this.cargandoDependencias = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoDependencias = false;
        this.toast.error('No se pudo cargar el catálogo de unidades. Intenta recargar la página.');
        this.cdr.detectChanges();
      },
    });
  }

  ngOnDestroy(): void {
    [...this.credencial, ...this.evidencias].forEach((a) => this.revocar(a));
  }

  // ---------------- errores por campo ----------------

  get errorNombre(): string | null {
    return Reglas.validarNombre(this.nombre, 'el nombre', true);
  }

  get errorApellido1(): string | null {
    return Reglas.validarNombre(this.apellido1, 'el primer apellido', true);
  }

  get errorApellido2(): string | null {
    return Reglas.validarNombre(this.apellido2, 'el segundo apellido', false);
  }

  get errorUnidad(): string | null {
    return this.unidadProcedenciaClave ? null : 'Selecciona tu unidad de procedencia de la lista.';
  }

  get etiquetaNumero(): string {
    return this.tipoIdentificacion === 'ALUMNO' ? 'Número de boleta' : 'Número de empleado';
  }

  get errorNumero(): string | null {
    const numero = this.numeroIdentificacion.trim();
    if (!numero) {
      return `Escribe tu ${this.etiquetaNumero.toLowerCase()}.`;
    }
    return Reglas.REGEX_NUMERO_IDENTIFICACION.test(numero)
      ? null
      : `${this.etiquetaNumero} solo admite dígitos (máximo ${Reglas.NUMERO_IDENTIFICACION_LONGITUD_MAXIMA}).`;
  }

  get errorDescripcion(): string | null {
    const largo = this.descripcionHechos.trim().length;
    if (largo < Reglas.DESCRIPCION_LONGITUD_MINIMA) {
      return `Describe los hechos con al menos ${Reglas.DESCRIPCION_LONGITUD_MINIMA} caracteres.`;
    }
    return largo > Reglas.DESCRIPCION_LONGITUD_MAXIMA
      ? `La descripción no puede exceder ${Reglas.DESCRIPCION_LONGITUD_MAXIMA} caracteres.`
      : null;
  }

  get errorCredencial(): string | null {
    return this.credencial.length === 0 ? 'Adjunta la imagen de tu credencial (frente y, si quieres, reverso).' : null;
  }

  get errorAviso(): string | null {
    return this.avisoAceptado ? null : 'Debes aceptar el aviso de privacidad para enviar tu respuesta.';
  }

  get errores(): string[] {
    return [
      this.errorNombre,
      this.errorApellido1,
      this.errorApellido2,
      this.errorUnidad,
      this.errorNumero,
      this.errorDescripcion,
      this.errorCredencial,
      this.errorAviso,
    ].filter((e): e is string => !!e);
  }

  get tamanioEvidencias(): number {
    return this.evidencias.reduce((total, a) => total + a.archivo.size, 0);
  }

  // ---------------- teclado ----------------

  soloLetras(evento: KeyboardEvent): void {
    if (evento.key.length === 1 && !/[A-Za-zÁÉÍÓÚÜÑáéíóúüñ '\-]/.test(evento.key)) {
      evento.preventDefault();
    }
  }

  soloDigitos(evento: KeyboardEvent): void {
    if (evento.key.length === 1 && !/\d/.test(evento.key)) {
      evento.preventDefault();
    }
  }

  // ---------------- archivos ----------------

  seleccionarCredencial(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    for (const archivo of Array.from(input.files ?? [])) {
      if (this.credencial.length >= Reglas.IDENTIFICACION_CANTIDAD_MAXIMA) {
        this.toast.advertencia(`Solo puedes adjuntar ${Reglas.IDENTIFICACION_CANTIDAD_MAXIMA} imágenes de tu credencial.`);
        break;
      }
      const extension = this.extension(archivo.name);
      if (!Reglas.IDENTIFICACION_EXTENSIONES.includes(extension)) {
        this.toast.error(`"${archivo.name}" no es JPG ni PNG. La credencial debe ser una imagen.`);
        continue;
      }
      if (archivo.size > Reglas.IDENTIFICACION_TAMANIO_MAXIMO) {
        this.toast.error(`"${archivo.name}" pesa más de 3 MB.`);
        continue;
      }
      this.credencial.push(this.aSeleccionado(archivo));
    }
    input.value = '';
    this.cdr.detectChanges();
  }

  seleccionarEvidencias(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    for (const archivo of Array.from(input.files ?? [])) {
      if (this.evidencias.length >= EVIDENCIA_CANTIDAD_MAXIMA) {
        this.toast.advertencia(`Puedes adjuntar máximo ${EVIDENCIA_CANTIDAD_MAXIMA} evidencias.`);
        break;
      }
      const extension = this.extension(archivo.name);
      if (!Reglas.EVIDENCIA_EXTENSIONES.includes(extension)) {
        this.toast.error(`"${archivo.name}" no es un tipo permitido (PDF, JPG, PNG, MP4 o MP3).`);
        continue;
      }
      if (archivo.size > Reglas.EVIDENCIA_TAMANIO_MAXIMO) {
        this.toast.error(`"${archivo.name}" pesa más de 30 MB.`);
        continue;
      }
      if (this.tamanioEvidencias + archivo.size > Reglas.EVIDENCIA_TAMANIO_TOTAL_MAXIMO) {
        this.toast.error('Las evidencias no pueden sumar más de 95 MB.');
        continue;
      }
      this.evidencias.push(this.aSeleccionado(archivo));
    }
    input.value = '';
    this.cdr.detectChanges();
  }

  quitar(lista: ArchivoSeleccionado[], indice: number): void {
    const [quitado] = lista.splice(indice, 1);
    if (quitado) {
      this.revocar(quitado);
    }
    this.cdr.detectChanges();
  }

  private aSeleccionado(archivo: File): ArchivoSeleccionado {
    const esImagen = archivo.type.startsWith('image/');
    return {
      archivo,
      vistaPrevia: esImagen ? URL.createObjectURL(archivo) : null,
      extension: this.extension(archivo.name).replace('.', '').toUpperCase() || 'ARCHIVO',
    };
  }

  private revocar(a: ArchivoSeleccionado): void {
    if (a.vistaPrevia) {
      URL.revokeObjectURL(a.vistaPrevia);
    }
  }

  private extension(nombre: string): string {
    const punto = nombre.lastIndexOf('.');
    return punto >= 0 ? nombre.slice(punto).toLowerCase() : '';
  }

  // ---------------- envío ----------------

  enviar(): void {
    this.intentoEnviar = true;
    this.errorEnvio = '';
    if (this.errores.length > 0) {
      this.toast.advertencia('Revisa los campos marcados antes de enviar.');
      this.cdr.detectChanges();
      return;
    }

    this.enviando = true;
    this.denunciadoService
      .registrarRespuesta(
        {
          folioQueja: this.folioQueja,
          nombre: this.nombre.trim(),
          apellido1: this.apellido1.trim(),
          apellido2: this.apellido2.trim(),
          unidadProcedenciaClave: this.unidadProcedenciaClave,
          tipoIdentificacion: this.tipoIdentificacion,
          numeroIdentificacion: this.numeroIdentificacion.trim(),
          descripcionHechos: this.descripcionHechos.trim(),
          avisoPrivacidadVersion: AVISO_DENUNCIADO_VERSION,
        },
        this.credencial.map((a) => a.archivo),
        this.evidencias.map((a) => a.archivo),
      )
      .subscribe({
        next: (respuesta) => {
          this.enviando = false;
          this.registrada = respuesta;
          window.scrollTo({ top: 0, behavior: 'smooth' });
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.enviando = false;
          this.errorEnvio =
            !err?.status || err.status >= 500
              ? 'No pudimos conectar con el servidor. Tus datos siguen aquí; intenta de nuevo en unos minutos.'
              : (err?.error?.mensaje ?? 'No se pudo registrar tu respuesta. Revisa los datos.');
          this.toast.error(this.errorEnvio);
          this.cdr.detectChanges();
        },
      });
  }
}
