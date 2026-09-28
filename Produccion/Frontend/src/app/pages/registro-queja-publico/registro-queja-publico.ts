import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CatalogoService } from '../../core/services/catalogo.service';
import { Dependencia } from '../../core/models/catalogo.models';
import { NotaFlotante } from '../../shared/nota-flotante/nota-flotante';
import { Datepicker } from '../../shared/datepicker/datepicker';
import { AvisoPrivacidad } from '../../shared/aviso-privacidad/aviso-privacidad';
import { AutocompletarDependencia } from '../../shared/autocompletar-dependencia/autocompletar-dependencia';
import { QuejaService } from '../../core/services/queja.service';
import { Queja } from '../../core/models/queja.models';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import * as Reglas from '../../core/validaciones/reglas-queja';

interface DatosTutor {
  nombre: string;
  apellido1: string;
  apellido2: string;
  parentesco: string;
  correo: string;
  telefono: string;
}

/**
 * Archivo seleccionado por el usuario junto con su miniatura.
 *
 * La miniatura se genera con URL.createObjectURL, que reserva memoria del navegador hasta
 * que se libera explícitamente — por eso cada URL se revoca al quitar el archivo y en
 * ngOnDestroy. Sin eso, seleccionar y quitar archivos repetidamente deja fugas.
 */
interface ArchivoPrevisualizado {
  archivo: File;
  /** Object URL de la miniatura; null cuando el archivo no es una imagen. */
  url: string | null;
  esImagen: boolean;
}

/** Caracteres que se permiten teclear en un campo de nombre/apellido. */
const CARACTER_NOMBRE = /[A-Za-zÁÉÍÓÚÜÑáéíóúüñ '\-]/;

@Component({
  selector: 'app-registro-queja-publico',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    NotaFlotante,
    Datepicker,
    AvisoPrivacidad,
    AutocompletarDependencia,
  ],
  templateUrl: './registro-queja-publico.html',
  styleUrl: './registro-queja-publico.scss',
})
export class RegistroQuejaPublico implements OnInit, OnDestroy {
  // Datos del quejoso
  nombre = '';
  apellido1 = '';
  apellido2 = '';
  correo = '';
  /** true cuando el correo tecleado ya tiene cuenta de seguimiento. */
  correoYaRegistrado = false;
  verificandoCorreo = false;
  private readonly authService = inject(AuthService);
  fechaNacimiento = '';
  identificacion: 'alumno' | 'empleado' = 'alumno';
  numeroBoletaEmpleado = '';

  // Datos de la queja
  unidadAcademica = '';
  fechaHechos = '';

  // Catálogo real de dependencias (para "lugar donde sucedieron los hechos") — se muestra
  // completo (las 208), sin filtrar por tipo, por decisión explícita del usuario.
  dependencias: Dependencia[] = [];
  cargandoDependencias = true;
  errorDependencias = '';

  // Datos del denunciado
  nombreDenunciado = '';
  apellido1Denunciado = '';
  apellido2Denunciado = '';
  descripcion = '';

  /** Archivos que sustentan la queja, con su miniatura. */
  evidencias: ArchivoPrevisualizado[] = [];
  /** Controla el estado visual del dropzone mientras se arrastra un archivo encima. */
  arrastrandoArchivo = false;

  /**
   * Foto o escaneo de la credencial de la comunidad politécnica — hasta 2 imágenes
   * (frente y reverso). Viajan como archivos de evidencia más, renombrados con el prefijo
   * "IDENTIFICACION_": el backend los reconoce por ese prefijo y les aplica reglas más
   * estrictas (solo JPG/PNG, 3MB, máximo 2) que al resto de evidencias, y los guarda con
   * tipo = "IDENTIFICACION" para que el recepcionista los distinga.
   */
  identificaciones: ArchivoPrevisualizado[] = [];

  // Aviso de privacidad — se muestra al entrar y bloquea el formulario hasta que se acepta.
  mostrarAvisoPrivacidad = true;
  avisoPrivacidadVersion = '';

  // Menor de edad / tutor
  mostrarModalTutor = false;
  /** true una vez que se confirmaron los datos del tutor — para mostrar el banner de
   * confirmación persistente que pidió el usuario ("que yo sepa que tengo datos de tutor"). */
  tutorConfirmado = false;
  /** true cuando el quejoso tiene menos de 14 años -- regla de negocio: no se bloquea la
   * queja por completo (el tutor puede presentarla), pero se bloquea el "autoregistro
   * directo": la mensajería deja explícito que debe completarla el padre/madre/tutor, y los
   * datos de tutor son obligatorios sin excepción (ver enviarQueja()). */
  esMenorDe14 = false;
  tutor: DatosTutor = {
    nombre: '',
    apellido1: '',
    apellido2: '',
    parentesco: 'Padre',
    correo: '',
    telefono: '',
  };

  cargando = false;
  /** true tras el primer intento de envío — a partir de ahí se muestran los mensajes de
   * validación de cada campo, aunque el usuario no lo haya "tocado" todavía. */
  intentoEnviar = false;
  quejaCreada: Queja | null = null;

  // ---- Límites que también ve la plantilla (espejo del backend, ver reglas-queja.ts) ----
  readonly fechaMaxima = Reglas.hoyIso();
  readonly fechaNacimientoMinima = Reglas.FECHA_NACIMIENTO_MINIMA;
  readonly fechaNacimientoMaxima = Reglas.fechaNacimientoMaxima();
  readonly nombreLongitudMaxima = Reglas.NOMBRE_LONGITUD_MAXIMA;
  readonly boletaLongitudMaxima = Reglas.NUMERO_IDENTIFICACION_LONGITUD_MAXIMA;
  readonly descripcionLongitudMinima = Reglas.DESCRIPCION_LONGITUD_MINIMA;
  readonly descripcionLongitudMaxima = Reglas.DESCRIPCION_LONGITUD_MAXIMA;
  readonly identificacionCantidadMaxima = Reglas.IDENTIFICACION_CANTIDAD_MAXIMA;

  constructor(
    private catalogoService: CatalogoService,
    private quejaService: QuejaService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cargandoDependencias = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoDependencias = false;
        this.errorDependencias =
          'No se pudo cargar el catálogo de dependencias. Intenta recargar la página.';
        this.toast.error(this.errorDependencias);
        this.cdr.detectChanges();
      },
    });
  }

  ngOnDestroy(): void {
    // Libera las miniaturas: los object URL viven hasta que se revocan, aunque el
    // componente ya no exista.
    [...this.identificaciones, ...this.evidencias].forEach((item) => this.liberarUrl(item));
  }

  // =========================================================================================
  // Aviso de privacidad
  // =========================================================================================

  onAvisoAceptado(version: string): void {
    this.avisoPrivacidadVersion = version;
    this.mostrarAvisoPrivacidad = false;
    this.cdr.detectChanges();
  }

  get avisoAceptado(): boolean {
    return this.avisoPrivacidadVersion !== '';
  }

  // =========================================================================================
  // Filtros de captura — impiden teclear caracteres que el campo no admite
  // =========================================================================================

  /** Bloquea todo lo que no sea letra, espacio, apóstrofe o guion en nombres y apellidos. */
  soloLetras(event: KeyboardEvent): void {
    if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !CARACTER_NOMBRE.test(event.key)) {
      event.preventDefault();
    }
  }

  /** Bloquea todo lo que no sea dígito en boleta / número de empleado. */
  soloDigitos(event: KeyboardEvent): void {
    if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !/[0-9]/.test(event.key)) {
      event.preventDefault();
    }
  }

  // =========================================================================================
  // Validaciones por campo — el mensaje que se pinta debajo de cada input
  // =========================================================================================

  get errorNombre(): string | null {
    return Reglas.validarNombre(this.nombre, 'tu nombre', true);
  }

  get errorApellido1(): string | null {
    return Reglas.validarNombre(this.apellido1, 'tu primer apellido', true);
  }

  get errorApellido2(): string | null {
    return Reglas.validarNombre(this.apellido2, 'tu segundo apellido', false);
  }

  get errorCorreo(): string | null {
    return Reglas.validarCorreo(this.correo);
  }

  get errorFechaNacimiento(): string | null {
    if (!this.fechaNacimiento) {
      return 'Selecciona tu fecha de nacimiento.';
    }
    if (!Reglas.esFechaValida(this.fechaNacimiento)) {
      return 'La fecha de nacimiento no es una fecha válida. Vuelve a seleccionarla.';
    }
    if (this.fechaNacimiento < this.fechaNacimientoMinima) {
      return 'La fecha de nacimiento no puede ser anterior al 1 de enero de 1920.';
    }
    if (this.fechaNacimiento > this.fechaNacimientoMaxima) {
      return `La fecha de nacimiento no puede ser posterior al 31 de diciembre de ${new Date().getFullYear() - 1}.`;
    }
    return null;
  }

  get errorBoleta(): string | null {
    const etiqueta = this.etiquetaNumeroIdentificacion;
    if (!this.numeroBoletaEmpleado) {
      return `Escribe tu ${etiqueta.toLowerCase()}.`;
    }
    if (!Reglas.REGEX_NUMERO_IDENTIFICACION.test(this.numeroBoletaEmpleado)) {
      return `${etiqueta} solo admite números, con un máximo de ${this.boletaLongitudMaxima} dígitos.`;
    }
    return null;
  }

  get errorIdentificacion(): string | null {
    return this.identificaciones.length === 0
      ? 'Adjunta la imagen de tu identificación oficial.'
      : null;
  }

  get errorUnidadAcademica(): string | null {
    return this.unidadAcademica
      ? null
      : 'Busca y elige de la lista el lugar donde sucedieron los hechos.';
  }

  get errorFechaHechos(): string | null {
    if (!this.fechaHechos) {
      return 'Selecciona la fecha de los hechos.';
    }
    if (!Reglas.esFechaValida(this.fechaHechos)) {
      return 'La fecha de los hechos no es una fecha válida. Vuelve a seleccionarla.';
    }
    if (this.fechaHechos > this.fechaMaxima) {
      return 'La fecha de los hechos no puede ser posterior a hoy.';
    }
    if (this.fechaNacimiento && this.fechaHechos < this.fechaNacimiento) {
      return 'La fecha de los hechos no puede ser anterior a tu fecha de nacimiento.';
    }
    return null;
  }

  get errorNombreDenunciado(): string | null {
    return Reglas.validarNombre(this.nombreDenunciado, 'el nombre del denunciado', false);
  }

  get errorApellido1Denunciado(): string | null {
    return Reglas.validarNombre(
      this.apellido1Denunciado,
      'el primer apellido del denunciado',
      false,
    );
  }

  get errorApellido2Denunciado(): string | null {
    return Reglas.validarNombre(
      this.apellido2Denunciado,
      'el segundo apellido del denunciado',
      false,
    );
  }

  get errorDescripcion(): string | null {
    const texto = this.descripcion.trim();
    if (!texto) {
      return 'Describe brevemente los hechos.';
    }
    if (texto.length < this.descripcionLongitudMinima) {
      return `Describe los hechos con al menos ${this.descripcionLongitudMinima} caracteres.`;
    }
    if (texto.length > this.descripcionLongitudMaxima) {
      return `La descripción no puede exceder ${this.descripcionLongitudMaxima} caracteres.`;
    }
    return null;
  }

  /** Todos los errores del formulario, en el orden en que aparecen en pantalla. */
  private get erroresFormulario(): (string | null)[] {
    return [
      this.errorNombre,
      this.errorApellido1,
      this.errorApellido2,
      this.errorCorreo,
      this.errorFechaNacimiento,
      this.errorBoleta,
      this.errorIdentificacion,
      this.errorUnidadAcademica,
      this.errorFechaHechos,
      this.errorNombreDenunciado,
      this.errorApellido1Denunciado,
      this.errorApellido2Denunciado,
      this.errorDescripcion,
    ];
  }

  /** Cambia según "alumno"/"empleado" para que el campo tenga el rótulo correcto. */
  get etiquetaNumeroIdentificacion(): string {
    return this.identificacion === 'alumno' ? 'Número de boleta' : 'Número de empleado';
  }

  // =========================================================================================
  // Identificación oficial — solo JPG/PNG, 3MB, hasta 2 imágenes
  // =========================================================================================

  onIdentificacionSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    this.procesarIdentificaciones(Array.from(input.files));
    input.value = '';
  }

  private procesarIdentificaciones(nuevos: File[]): void {
    for (const archivo of nuevos) {
      if (this.identificaciones.length >= Reglas.IDENTIFICACION_CANTIDAD_MAXIMA) {
        this.toast.error(
          `Solo puedes adjuntar hasta ${Reglas.IDENTIFICACION_CANTIDAD_MAXIMA} imágenes de tu identificación.`,
        );
        return;
      }
      if (!this.esImagenPermitida(archivo)) {
        this.toast.error(
          `"${archivo.name}" no es una imagen. La identificación oficial debe ser JPG o PNG (ya no se acepta PDF).`,
        );
        continue;
      }
      if (archivo.size > Reglas.IDENTIFICACION_TAMANIO_MAXIMO) {
        this.toast.error(
          `"${archivo.name}" pesa ${Reglas.formatearTamanio(archivo.size)}; el máximo para la identificación es 3MB.`,
        );
        continue;
      }
      this.identificaciones.push(this.conMiniatura(archivo));
    }
  }

  quitarIdentificacion(indice: number): void {
    const [quitado] = this.identificaciones.splice(indice, 1);
    this.liberarUrl(quitado);
  }

  /** Comprueba tipo MIME y extensión: algunos navegadores no reportan el MIME correctamente. */
  private esImagenPermitida(archivo: File): boolean {
    const extension = '.' + (archivo.name.split('.').pop()?.toLowerCase() ?? '');
    return (
      Reglas.IDENTIFICACION_TIPOS_MIME.includes(archivo.type) ||
      Reglas.IDENTIFICACION_EXTENSIONES.includes(extension)
    );
  }

  // =========================================================================================
  // Evidencias
  // =========================================================================================

  onArchivosSeleccionados(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    this.procesarArchivosNuevos(Array.from(input.files));
    input.value = '';
  }

  onArrastreSobre(event: DragEvent): void {
    event.preventDefault();
    this.arrastrandoArchivo = true;
  }

  onArrastreSale(): void {
    this.arrastrandoArchivo = false;
  }

  onArchivosSoltados(event: DragEvent): void {
    event.preventDefault();
    this.arrastrandoArchivo = false;
    if (!event.dataTransfer?.files) return;
    this.procesarArchivosNuevos(Array.from(event.dataTransfer.files));
  }

  /** Valida tipo, tamaño individual y tamaño total ANTES de agregar cada archivo a la
   * lista — así el usuario se entera al momento (con un toast claro) en vez de enviarlo
   * todo y esperar a que el servidor lo rechace con un 400/413. */
  private procesarArchivosNuevos(nuevos: File[]): void {
    for (const archivo of nuevos) {
      const extension = '.' + (archivo.name.split('.').pop()?.toLowerCase() ?? '');
      if (!Reglas.EVIDENCIA_EXTENSIONES.includes(extension)) {
        this.toast.error(
          `"${archivo.name}" no es un tipo de archivo permitido. Usa PDF, JPG, PNG, MP4 o MP3.`,
        );
        continue;
      }
      if (archivo.size > Reglas.EVIDENCIA_TAMANIO_MAXIMO) {
        this.toast.error(
          `"${archivo.name}" pesa ${Reglas.formatearTamanio(archivo.size)}; el máximo por archivo es 30MB.`,
        );
        continue;
      }
      const tamanioActual = this.evidencias.reduce((total, a) => total + a.archivo.size, 0);
      if (tamanioActual + archivo.size > Reglas.EVIDENCIA_TAMANIO_TOTAL_MAXIMO) {
        this.toast.error(
          `No se agregó "${archivo.name}": el total de archivos adjuntos no puede superar 100MB.`,
        );
        continue;
      }
      this.evidencias.push(this.conMiniatura(archivo));
    }
  }

  quitarArchivo(indice: number): void {
    const [quitado] = this.evidencias.splice(indice, 1);
    this.liberarUrl(quitado);
  }

  // =========================================================================================
  // Miniaturas
  // =========================================================================================

  private conMiniatura(archivo: File): ArchivoPrevisualizado {
    const esImagen = archivo.type.startsWith('image/');
    return {
      archivo,
      esImagen,
      url: esImagen ? URL.createObjectURL(archivo) : null,
    };
  }

  private liberarUrl(item: ArchivoPrevisualizado | undefined): void {
    if (item?.url) {
      URL.revokeObjectURL(item.url);
    }
  }

  /** Etiqueta corta del tipo de archivo, para los adjuntos que no son imagen. */
  tipoArchivo(archivo: File): string {
    const extension = (archivo.name.split('.').pop() ?? '').toUpperCase();
    return extension || 'ARCHIVO';
  }

  formatearTamanio(bytes: number): string {
    return Reglas.formatearTamanio(bytes);
  }

  // =========================================================================================
  // Menor de edad / tutor
  // =========================================================================================

  onFechaNacimientoChange(): void {
    if (!this.fechaNacimiento) return;
    // Con una fecha inválida el cálculo de edad daría NaN y el modal de tutor se abriría o no
    // al azar; mejor detenerse aquí y avisar.
    if (this.errorFechaNacimiento) {
      this.toast.error(this.errorFechaNacimiento);
      return;
    }
    const edad = this.calcularEdad(this.fechaNacimiento);
    if (edad < 18) {
      this.esMenorDe14 = edad < 14;
      this.tutorConfirmado = false;
      this.mostrarModalTutor = true;
      this.toast.advertencia(
        this.esMenorDe14
          ? 'Por tratarse de una persona menor de 14 años, este formulario debe completarlo su padre, madre o tutor.'
          : 'Por ser menor de edad, necesitamos los datos de un tutor o adulto responsable.',
      );
    } else {
      this.esMenorDe14 = false;
      this.tutorConfirmado = false;
    }
  }

  private calcularEdad(fechaISO: string): number {
    const nacimiento = new Date(fechaISO);
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();
    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  }

  confirmarTutor(): void {
    if (!this.tutor.nombre || !this.tutor.apellido1 || !this.tutor.parentesco) {
      this.toast.advertencia('Completa al menos nombre, primer apellido y parentesco del tutor.');
      return;
    }
    const errorNombreTutor =
      Reglas.validarNombre(this.tutor.nombre, 'el nombre del tutor', true) ??
      Reglas.validarNombre(this.tutor.apellido1, 'el primer apellido del tutor', true) ??
      Reglas.validarNombre(this.tutor.apellido2, 'el segundo apellido del tutor', false);
    if (errorNombreTutor) {
      this.toast.advertencia(errorNombreTutor);
      return;
    }
    if (this.tutor.correo) {
      const errorCorreoTutor = Reglas.validarCorreo(this.tutor.correo);
      if (errorCorreoTutor) {
        this.toast.advertencia(`Correo del tutor: ${errorCorreoTutor}`);
        return;
      }
    }
    if (this.tutor.telefono && !/^\d{10}$/.test(this.tutor.telefono)) {
      this.toast.advertencia('El teléfono del tutor debe tener exactamente 10 dígitos.');
      return;
    }
    this.mostrarModalTutor = false;
    this.tutorConfirmado = true;
    this.toast.exito('Datos del tutor guardados.');
  }

  cancelarTutor(): void {
    // Antes esto borraba fechaNacimiento y "desconfirmaba" el tutor sin importar nada,
    // así que si el usuario abría "Editar tutor" para corregir un dato y luego cancelaba,
    // perdía TODO el progreso (incluida su fecha de nacimiento ya capturada) aunque ya
    // hubiera confirmado los datos del tutor antes. Cancelar ahora solo cierra el modal —
    // no toca fechaNacimiento ni tutorConfirmado. Si el tutor nunca se confirmó, el
    // bloqueo real ocurre al momento de enviar (ver enviarQueja()).
    this.mostrarModalTutor = false;
  }

  editarTutor(): void {
    this.mostrarModalTutor = true;
  }

  // =========================================================================================
  // Envío
  // =========================================================================================

  enviarQueja(): void {
    this.intentoEnviar = true;

    if (!this.avisoAceptado) {
      this.mostrarAvisoPrivacidad = true;
      this.toast.error('Debes leer y aceptar el aviso de privacidad antes de enviar tu queja.');
      return;
    }

    const primerError = this.erroresFormulario.find((error) => error !== null);
    if (primerError) {
      this.toast.error(primerError);
      return;
    }

    if (this.mostrarModalTutor) {
      this.toast.advertencia(
        'Termina de confirmar o cancelar los datos del tutor antes de continuar.',
      );
      return;
    }

    // Antes solo se bloqueaba el envío si el modal seguía ABIERTO -- si el usuario lo
    // cancelaba, la queja de un menor se podía enviar sin ningún dato de tutor. Ahora, si la
    // fecha de nacimiento indica que es menor de edad, los datos de tutor son obligatorios
    // sin excepción (para menores de 14 años esto es justamente lo que hace que el trámite
    // solo pueda completarse a través de su tutor, no directamente).
    if (this.calcularEdad(this.fechaNacimiento) < 18 && !this.tutorConfirmado) {
      this.toast.error(
        'Debes completar los datos del tutor o adulto responsable antes de enviar la queja.',
      );
      this.mostrarModalTutor = true;
      return;
    }

    this.cargando = true;
    this.quejaService
      .registrarQuejaPublica({
        nombre: this.nombre.trim(),
        apellido1: this.apellido1.trim(),
        apellido2: this.apellido2.trim() || undefined,
        correo: this.correo.trim().toLowerCase(),
        fechaNacimiento: this.fechaNacimiento,
        tipoIdentificacion: this.identificacion,
        numeroIdentificacion: this.numeroBoletaEmpleado,
        unidadAcademicaClave: this.unidadAcademica,
        fechaHechos: this.fechaHechos,
        nombreDenunciado: this.nombreDenunciado.trim() || undefined,
        apellido1Denunciado: this.apellido1Denunciado.trim() || undefined,
        apellido2Denunciado: this.apellido2Denunciado.trim() || undefined,
        descripcion: this.descripcion.trim(),
        archivos: this.archivosParaEnviar(),
        avisoPrivacidadAceptado: true,
        avisoPrivacidadVersion: this.avisoPrivacidadVersion,
        tutor: this.tutorConfirmado ? this.tutor : undefined,
      })
      .subscribe({
        next: (queja) => {
          this.cargando = false;
          this.quejaCreada = queja;
          this.toast.exito('¡Queja registrada correctamente!');
          // Forzamos el refresco de esta vista de inmediato: la app corre sin zone.js
          // (zoneless), así que una respuesta HTTP asíncrona por sí sola no basta para que
          // Angular vuelva a revisar esta plantilla — sin esto el botón se queda para
          // siempre en "Enviando…" aunque los datos ya se hayan actualizado por dentro.
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.cargando = false;
          const mensaje =
            err?.error?.mensaje ?? 'No se pudo registrar la queja. Intenta de nuevo.';
          this.toast.error(mensaje);
          this.cdr.detectChanges();
        },
      });
  }

  /**
   * Las credenciales viajan renombradas con el prefijo IDENTIFICACION_ dentro de la misma
   * lista de archivos: es lo que le permite al backend aplicarles reglas más estrictas y
   * guardarlas con tipo = "IDENTIFICACION" sin cambiar el contrato multipart del endpoint.
   */
  private archivosParaEnviar(): File[] {
    const credenciales = this.identificaciones.map(
      (item) =>
        new File([item.archivo], `${Reglas.PREFIJO_IDENTIFICACION}${item.archivo.name}`, {
          type: item.archivo.type,
        }),
    );
    return [...credenciales, ...this.evidencias.map((item) => item.archivo)];
  }

  /** Abre una ventana con un acuse imprimible (el usuario lo guarda como PDF desde el
   * diálogo de impresión del navegador — no hay generación de PDF en servidor todavía). */
  descargarAcuse(): void {
    if (!this.quejaCreada) return;

    const ventana = window.open('', '_blank', 'width=650,height=800');
    if (!ventana) {
      this.toast.error(
        'Tu navegador bloqueó la ventana de impresión. Habilita las ventanas emergentes para descargar el acuse.',
      );
      return;
    }

    const q = this.quejaCreada;
    const folio = this.escaparHtml(q.numeroFolio);
    const correo = this.escaparHtml(q.correoInstitucional);
    const motivo = q.motivo ? this.escaparHtml(q.motivo) : '';
    const nombreCompleto = [q.nombreQuejoso, q.apellido1Quejoso, q.apellido2Quejoso]
      .filter(Boolean)
      .map((v) => this.escaparHtml(v as string))
      .join(' ');
    const fechaNacimiento = q.fechaNacimientoQuejoso ? this.formatearFecha(q.fechaNacimientoQuejoso) : '';
    const tipoIdentificacion =
      q.tipoIdentificacionQuejoso === 'empleado' ? 'Personal administrativo (empleado)' : 'Alumno';
    const identificacion = q.numeroIdentificacionQuejoso
      ? `${q.tipoIdentificacionQuejoso === 'empleado' ? 'Núm. de empleado' : 'Núm. de boleta'}: ${this.escaparHtml(q.numeroIdentificacionQuejoso)}`
      : '';
    const unidadAcademica = q.unidadAcademicaClave ? this.escaparHtml(q.unidadAcademicaClave) : '';
    const fechaHechos = q.fechaHechos ? this.formatearFecha(q.fechaHechos) : '';
    const denunciado = [q.nombreDenunciado, q.apellido1Denunciado, q.apellido2Denunciado]
      .filter(Boolean)
      .map((v) => this.escaparHtml(v as string))
      .join(' ');
    const descripcion = q.descripcion ? this.escaparHtml(q.descripcion).replace(/\n/g, '<br>') : '';
    const fechaEmision = new Date().toLocaleString('es-MX', {
      dateStyle: 'long',
      timeStyle: 'short',
    });

    // Nombres de los archivos que se acaban de enviar con esta queja -- la respuesta del
    // backend (Queja) no trae la lista de evidencias, pero siguen disponibles aquí en el
    // estado del componente justo después de un envío exitoso.
    const nombresEvidencias = [
      ...this.identificaciones.map(
        (item) => `${Reglas.PREFIJO_IDENTIFICACION}${item.archivo.name}`,
      ),
      ...this.evidencias.map((item) => item.archivo.name),
    ];
    const evidenciasHtml = nombresEvidencias.length
      ? `<ul class="lista">${nombresEvidencias.map((n) => `<li>${this.escaparHtml(n)}</li>`).join('')}</ul>`
      : '<p class="vacio">Sin archivos adjuntos.</p>';

    const fila = (etiqueta: string, valor: string) =>
      valor ? `<tr><td>${etiqueta}</td><td>${valor}</td></tr>` : '';

    const tutorHtml = this.tutorConfirmado
      ? `
        <h2 class="seccion">Tutor / adulto responsable</h2>
        <table>
          ${fila('Nombre', this.escaparHtml([this.tutor.nombre, this.tutor.apellido1, this.tutor.apellido2].filter(Boolean).join(' ')))}
          ${fila('Parentesco', this.escaparHtml(this.tutor.parentesco))}
          ${fila('Correo', this.tutor.correo ? this.escaparHtml(this.tutor.correo) : '')}
          ${fila('Teléfono', this.tutor.telefono ? this.escaparHtml(this.tutor.telefono) : '')}
        </table>`
      : '';

    ventana.document.write(`
      <html>
        <head>
          <title>Acuse de Recibo - ${folio}</title>
          <style>
            * { box-sizing: border-box; }
            body {
              font-family: 'Segoe UI', Arial, sans-serif; padding: 0; margin: 0; color: #1a1a1a;
              background: #f5f5f5;
            }
            .hoja { max-width: 680px; margin: 24px auto; background: #fff; padding: 40px 44px; }
            .membrete {
              display: flex; align-items: center; gap: 14px; border-bottom: 3px solid #6c1d45;
              padding-bottom: 16px; margin-bottom: 24px;
            }
            .membrete .escudo {
              width: 44px; height: 44px; border-radius: 50%; background: #6c1d45; color: #fff;
              display: flex; align-items: center; justify-content: center; font-weight: 700;
              font-size: 1.1rem; flex-shrink: 0;
            }
            .membrete h1 { color: #6c1d45; font-size: 1.15rem; margin: 0; line-height: 1.3; }
            .membrete p { margin: 2px 0 0; font-size: 0.8rem; color: #666; }
            .titulo-doc {
              text-align: center; text-transform: uppercase; letter-spacing: 1px;
              font-size: 0.85rem; color: #666; margin-bottom: 4px;
            }
            .folio {
              font-size: 1.7rem; font-weight: 700; letter-spacing: 1.5px; margin: 8px 0 8px;
              border: 2px solid #6c1d45; padding: 14px; text-align: center; border-radius: 10px;
              color: #6c1d45; background: #f7eef1;
            }
            .estatus {
              display: block; width: fit-content; margin: 0 auto 28px; padding: 4px 14px;
              border-radius: 999px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase;
              letter-spacing: 0.04em; background: #e3f2fd; color: #1565c0;
            }
            h2.seccion {
              font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.6px; color: #9d2449;
              border-bottom: 1px solid #eee; padding-bottom: 6px; margin: 22px 0 8px;
            }
            table { width: 100%; border-collapse: collapse; }
            td { padding: 7px 4px; border-bottom: 1px solid #f0f0f0; font-size: 0.88rem; }
            td:first-child { font-weight: 600; width: 220px; color: #444; }
            .descripcion {
              font-size: 0.88rem; line-height: 1.6; background: #fafafa; border: 1px solid #eee;
              border-radius: 6px; padding: 12px 14px; margin: 0; white-space: pre-line;
            }
            .lista { margin: 0; padding-left: 18px; font-size: 0.85rem; line-height: 1.7; }
            .vacio { font-size: 0.85rem; color: #999; margin: 4px 0; }
            .pie {
              margin-top: 34px; font-size: 0.75rem; color: #666; border-top: 1px dashed #ccc;
              padding-top: 14px; line-height: 1.5;
            }
            .emision { margin-top: 6px; font-size: 0.72rem; color: #999; }
            @media print {
              body { background: #fff; }
              .hoja { margin: 0; padding: 20px; }
            }
          </style>
        </head>
        <body>
          <div class="hoja">
            <div class="membrete">
              <div class="escudo">DDP</div>
              <div>
                <h1>Defensoría de los Derechos Politécnicos</h1>
                <p>Instituto Politécnico Nacional</p>
              </div>
            </div>

            <p class="titulo-doc">Acuse de recibo de queja</p>
            <div class="folio">${folio}</div>
            <span class="estatus">Recibida</span>

            <h2 class="seccion">Datos del quejoso</h2>
            <table>
              ${fila('Nombre', nombreCompleto)}
              ${fila('Correo', correo)}
              ${fila('Fecha de nacimiento', fechaNacimiento)}
              ${fila('Calidad', this.escaparHtml(tipoIdentificacion))}
              ${fila('Identificación', identificacion)}
            </table>

            <h2 class="seccion">Lugar de los hechos</h2>
            <table>
              ${fila('Motivo', motivo)}
              ${fila('Lugar de los hechos', unidadAcademica)}
              ${fila('Fecha de los hechos', fechaHechos)}
              ${fila('Persona denunciada', denunciado)}
            </table>

            ${descripcion ? `<h2 class="seccion">Descripción de los hechos</h2><p class="descripcion">${descripcion}</p>` : ''}

            <h2 class="seccion">Evidencias adjuntas</h2>
            ${evidenciasHtml}

            ${tutorHtml}

            <p class="pie">
              Guarda este acuse. Necesitarás el <strong>folio</strong> y el <strong>correo
              registrado</strong> para dar seguimiento a tu trámite en cualquier momento desde
              "Consultar folio". Las quejas sobre hechos ocurridos hace más de 90 días no serán
              procesadas.
            </p>
            <p class="emision">
              Documento generado el ${fechaEmision}.
              Aviso de privacidad aceptado, versión ${this.escaparHtml(this.avisoPrivacidadVersion)}.
            </p>
          </div>
        </body>
      </html>
    `);
    ventana.document.close();
    ventana.focus();
    setTimeout(() => ventana.print(), 300);
  }

  private escaparHtml(valor: string): string {
    const div = document.createElement('div');
    div.textContent = valor;
    return div.innerHTML;
  }

  /** "2026-08-23" -> "23/08/2026" -- las fechas llegan en formato ISO (yyyy-mm-dd); se
   * muestran así en el acuse porque es el formato que espera cualquier persona que lo lea. */
  private formatearFecha(fechaISO: string): string {
    const partes = fechaISO.split('-');
    if (partes.length !== 3) return this.escaparHtml(fechaISO);
    const [anio, mes, dia] = partes;
    return `${dia}/${mes}/${anio}`;
  }

  /**
   * Al salir del campo de correo revisa si ya existe una cuenta con ese correo. Si existe,
   * el formulario muestra el aviso para que el quejoso inicie sesion y levante la queja
   * desde su panel (asi la queja queda ligada a su cuenta). No bloquea el envio.
   */
  verificarCorreoRegistrado(): void {
    const correo = this.correo.trim().toLowerCase();
    this.correoYaRegistrado = false;

    if (!correo || Reglas.validarCorreo(correo)) {
      return;
    }

    this.verificandoCorreo = true;
    this.authService.existeCuenta(correo).subscribe({
      next: (resp) => {
        this.correoYaRegistrado = resp.existe;
        this.verificandoCorreo = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // Si la consulta falla no estorbamos: el quejoso puede seguir con su queja.
        this.verificandoCorreo = false;
        this.cdr.detectChanges();
      },
    });
  }
}
