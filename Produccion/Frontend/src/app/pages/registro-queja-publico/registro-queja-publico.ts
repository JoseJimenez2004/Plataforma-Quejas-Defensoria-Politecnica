import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CatalogoService } from '../../core/services/catalogo.service';
import { Dependencia } from '../../core/models/catalogo.models';
import { NotaFlotante } from '../../shared/nota-flotante/nota-flotante';
import { Datepicker } from '../../shared/datepicker/datepicker';
import { QuejaService } from '../../core/services/queja.service';
import { Queja } from '../../core/models/queja.models';
import { ToastService } from '../../core/services/toast.service';

interface DatosTutor {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  parentesco: string;
  correo: string;
  telefono: string;
}

/** Extensiones aceptadas como evidencia — deben coincidir con el texto que ve el usuario
 * ("PDF, JPG, PNG, MP4, MP3") y con lo que realmente acepta el backend. */
const EXTENSIONES_PERMITIDAS = ['.pdf', '.jpg', '.jpeg', '.png', '.mp4', '.mp3'];
/** Debe coincidir con `max-file-size` en quejas-service.yml (backend). */
const TAMANIO_MAX_ARCHIVO = 30 * 1024 * 1024;
/** Debe quedar por debajo de `max-request-size` (100MB) del backend, dejando margen para el
 * resto de los campos del formulario y el overhead de multipart. */
const TAMANIO_MAX_TOTAL = 95 * 1024 * 1024;

@Component({
  selector: 'app-registro-queja-publico',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NotaFlotante, Datepicker],
  templateUrl: './registro-queja-publico.html',
  styleUrl: './registro-queja-publico.scss',
})
export class RegistroQuejaPublico implements OnInit {
  // Datos del quejoso
  nombre = '';
  apellidoPaterno = '';
  apellidoMaterno = '';
  correo = '';
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
  apellidoDenunciado = '';
  segundoApellidoDenunciado = '';
  descripcion = '';
  archivos: File[] = [];
  /** Controla el estado visual del dropzone mientras se arrastra un archivo encima. */
  arrastrandoArchivo = false;

  /** Foto/escaneo de la credencial de la comunidad politécnica -- se manda como un archivo
   * de evidencia más (el backend no distingue "tipos" de evidencia todavía), pero renombrado
   * con el prefijo "IDENTIFICACION_" para que el recepcionista lo identifique de un vistazo
   * al validar la queja. Ver docs sobre validación de credencial: por ahora es validación
   * humana (Nivel 1), no hay OCR/comparación automática. */
  identificacionArchivo: File | null = null;

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
    apellidoPaterno: '',
    apellidoMaterno: '',
    parentesco: 'Padre',
    correo: '',
    telefono: '',
  };

  cargando = false;
  /** true tras el primer intento de envío — a partir de ahí se muestran los mensajes de
   * validación de cada campo, aunque el usuario no lo haya "tocado" todavía. */
  intentoEnviar = false;
  quejaCreada: Queja | null = null;

  /** Fecha máxima seleccionable (hoy) para nacimiento y hechos — el calendario nativo no
   * debería dejar elegir fechas futuras. */
  readonly fechaMaxima = new Date().toISOString().split('T')[0];

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

  /** Cambia según "alumno"/"empleado" para que el campo tenga el rótulo correcto. */
  get etiquetaNumeroIdentificacion(): string {
    return this.identificacion === 'alumno' ? 'Número de boleta' : 'Número de empleado';
  }

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
      if (!EXTENSIONES_PERMITIDAS.includes(extension)) {
        this.toast.error(
          `"${archivo.name}" no es un tipo de archivo permitido. Usa PDF, JPG, PNG, MP4 o MP3.`,
        );
        continue;
      }
      if (archivo.size > TAMANIO_MAX_ARCHIVO) {
        this.toast.error(
          `"${archivo.name}" pesa ${this.formatearTamanio(archivo.size)}; el máximo por archivo es 30MB.`,
        );
        continue;
      }
      const tamanioActual = this.archivos.reduce((total, a) => total + a.size, 0);
      if (tamanioActual + archivo.size > TAMANIO_MAX_TOTAL) {
        this.toast.error(
          `No se agregó "${archivo.name}": el total de archivos adjuntos no puede superar 100MB.`,
        );
        continue;
      }
      this.archivos.push(archivo);
    }
  }

  quitarArchivo(indice: number): void {
    this.archivos.splice(indice, 1);
  }

  onIdentificacionSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;

    const extension = '.' + (archivo.name.split('.').pop()?.toLowerCase() ?? '');
    if (!['.pdf', '.jpg', '.jpeg', '.png'].includes(extension)) {
      this.toast.error('La identificación debe ser un archivo PDF, JPG o PNG.');
      return;
    }
    if (archivo.size > TAMANIO_MAX_ARCHIVO) {
      this.toast.error(
        `"${archivo.name}" pesa ${this.formatearTamanio(archivo.size)}; el máximo es 30MB.`,
      );
      return;
    }
    this.identificacionArchivo = archivo;
  }

  quitarIdentificacion(): void {
    this.identificacionArchivo = null;
  }

  formatearTamanio(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  onFechaNacimientoChange(): void {
    if (!this.fechaNacimiento) return;
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
    if (!this.tutor.nombre || !this.tutor.apellidoPaterno || !this.tutor.parentesco) {
      this.toast.advertencia('Completa al menos nombre, apellido paterno y parentesco del tutor.');
      return;
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

  enviarQueja(): void {
    this.intentoEnviar = true;

    if (
      !this.nombre ||
      !this.apellidoPaterno ||
      !this.correo ||
      !this.fechaNacimiento ||
      !this.numeroBoletaEmpleado ||
      !this.unidadAcademica ||
      !this.fechaHechos ||
      !this.descripcion
    ) {
      this.toast.error('Completa todos los campos obligatorios antes de enviar la queja.');
      return;
    }

    if (!/^\d+$/.test(this.numeroBoletaEmpleado)) {
      this.toast.error(`${this.etiquetaNumeroIdentificacion} solo puede contener números.`);
      return;
    }

    if (!this.identificacionArchivo) {
      this.toast.error('Adjunta tu identificación oficial de la comunidad politécnica.');
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
    if (this.fechaNacimiento && this.calcularEdad(this.fechaNacimiento) < 18 && !this.tutorConfirmado) {
      this.toast.error(
        'Debes completar los datos del tutor o adulto responsable antes de enviar la queja.',
      );
      this.mostrarModalTutor = true;
      return;
    }

    // La credencial se manda como un archivo de evidencia más (el backend aún no distingue
    // "tipos" de evidencia), pero renombrada con el prefijo IDENTIFICACION_ para que el
    // recepcionista la reconozca de inmediato al validar la queja.
    const archivoIdentificacion = new File(
      [this.identificacionArchivo],
      `IDENTIFICACION_${this.identificacionArchivo.name}`,
      { type: this.identificacionArchivo.type },
    );

    this.cargando = true;
    this.quejaService
      .registrarQuejaPublica({
        nombre: this.nombre,
        apellidoPaterno: this.apellidoPaterno,
        apellidoMaterno: this.apellidoMaterno || undefined,
        correo: this.correo,
        fechaNacimiento: this.fechaNacimiento,
        tipoIdentificacion: this.identificacion,
        numeroIdentificacion: this.numeroBoletaEmpleado,
        unidadAcademicaClave: this.unidadAcademica,
        fechaHechos: this.fechaHechos,
        nombreDenunciado: this.nombreDenunciado || undefined,
        apellidoDenunciado: this.apellidoDenunciado || undefined,
        descripcion: this.descripcion,
        archivos: [archivoIdentificacion, ...this.archivos],
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
    const nombreCompleto = [q.nombreQuejoso, q.apellidoPaternoQuejoso, q.apellidoMaternoQuejoso]
      .filter(Boolean)
      .map((v) => this.escaparHtml(v as string))
      .join(' ');
    const identificacion = q.numeroIdentificacionQuejoso
      ? `${q.tipoIdentificacionQuejoso === 'empleado' ? 'Núm. de empleado' : 'Núm. de boleta'}: ${this.escaparHtml(q.numeroIdentificacionQuejoso)}`
      : '';
    const unidadAcademica = q.unidadAcademicaClave ? this.escaparHtml(q.unidadAcademicaClave) : '';
    const fechaHechos = q.fechaHechos ? this.escaparHtml(q.fechaHechos) : '';
    const denunciado = [q.nombreDenunciado, q.apellidoDenunciado]
      .filter(Boolean)
      .map((v) => this.escaparHtml(v as string))
      .join(' ');
    const fechaEmision = new Date().toLocaleString('es-MX', {
      dateStyle: 'long',
      timeStyle: 'short',
    });

    const fila = (etiqueta: string, valor: string) =>
      valor ? `<tr><td>${etiqueta}</td><td>${valor}</td></tr>` : '';

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
              font-size: 1.7rem; font-weight: 700; letter-spacing: 1.5px; margin: 8px 0 28px;
              border: 2px solid #6c1d45; padding: 14px; text-align: center; border-radius: 10px;
              color: #6c1d45; background: #f7eef1;
            }
            h2.seccion {
              font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.6px; color: #9d2449;
              border-bottom: 1px solid #eee; padding-bottom: 6px; margin: 22px 0 8px;
            }
            table { width: 100%; border-collapse: collapse; }
            td { padding: 7px 4px; border-bottom: 1px solid #f0f0f0; font-size: 0.88rem; }
            td:first-child { font-weight: 600; width: 220px; color: #444; }
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

            <h2 class="seccion">Datos del quejoso</h2>
            <table>
              ${fila('Nombre', nombreCompleto)}
              ${fila('Correo', correo)}
              ${fila('Identificación', identificacion)}
            </table>

            <h2 class="seccion">Datos de la queja</h2>
            <table>
              ${fila('Motivo', motivo)}
              ${fila('Lugar de los hechos', unidadAcademica)}
              ${fila('Fecha de los hechos', fechaHechos)}
              ${fila('Persona denunciada', denunciado)}
            </table>

            <p class="pie">
              Guarda este acuse. Necesitarás el <strong>folio</strong> y el <strong>correo
              registrado</strong> para dar seguimiento a tu trámite en cualquier momento desde
              "Consultar folio". Las quejas sobre hechos ocurridos hace más de 90 días no serán
              procesadas.
            </p>
            <p class="emision">Documento generado el ${fechaEmision}.</p>
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
}
