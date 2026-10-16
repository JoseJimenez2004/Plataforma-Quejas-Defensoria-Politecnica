import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { EvidenciaResumen, IdentificacionOficialOpcion, QuejaDetalle } from '../../core/models/revision.models';

/** Plazo para presentar la queja, en días naturales, contado desde la fecha de los hechos
 * hasta la fecha en que se registró la queja. */
export const PLAZO_DIAS_QUEJA = 90;

/** Claves de las preguntas de la lista de verificación, en el orden en que se muestran. */
type ClavePregunta =
  | 'nombreCompleto'
  | 'identificacionOficial'
  | 'numeroIdentificacion'
  /** Sub-pregunta del requisito 3: solo aparece si la boleta es "No aplicable". */
  | 'numeroEmpleado'
  | 'fechaHechos'
  | 'datosDenunciado';

/** true = Sí, false = No, 'na' = No aplicable (solo la pregunta de la boleta), null = sin contestar. */
type ValorRespuesta = boolean | 'na' | null;

interface RespuestaPregunta {
  valor: ValorRespuesta;
  observacion: string;
}

/** Texto del motivo que se le manda al quejoso cuando la respuesta es "No". */
const MOTIVO_SI_NO: Record<ClavePregunta, string> = {
  nombreCompleto: 'El nombre asentado en la queja no coincide íntegramente con la identificación oficial adjunta.',
  identificacionOficial:
    'No se exhibió una identificación oficial con fotografía válida y vigente (o, en su caso, la constancia idónea de representación legal), en términos del Artículo 13 de la Gaceta Politécnica y del apartado de Control y Gestión Administrativa de Documentos.',
  numeroIdentificacion: 'El número de boleta registrado no concuerda con la constancia o credencial institucional aportada.',
  numeroEmpleado: 'El número de empleado registrado no concuerda con la constancia o credencial institucional aportada.',
  fechaHechos: `La queja no fue promovida dentro del término de ${PLAZO_DIAS_QUEJA} días naturales posteriores a los hechos.`,
  datosDenunciado: 'La queja no aporta elementos suficientes para la plena identificación de la persona señalada.',
};

/** Encabezado corto de cada requisito, para armar las observaciones del correo. */
const TITULO_PREGUNTA: Record<ClavePregunta, string> = {
  nombreCompleto: 'Identidad de la persona quejosa',
  identificacionOficial: 'Documentación de identidad y personalidad jurídica',
  numeroIdentificacion: 'Matrícula institucional (boleta)',
  numeroEmpleado: 'Matrícula institucional (número de empleado)',
  fechaHechos: 'Oportunidad en la presentación',
  datosDenunciado: 'Identificación de la persona señalada',
};

const ORDEN_PREGUNTAS: ClavePregunta[] = [
  'nombreCompleto',
  'identificacionOficial',
  'numeroIdentificacion',
  'fechaHechos',
  'datosDenunciado',
];

@Component({
  selector: 'app-validacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './validacion.html',
  styleUrl: './validacion.scss',
})
export class Validacion implements OnInit, OnDestroy {
  readonly plazoDias = PLAZO_DIAS_QUEJA;

  folio = '';
  queja: QuejaDetalle | null = null;
  cargando = true;
  enviando = false;

  respuestas: Record<ClavePregunta, RespuestaPregunta> = {
    nombreCompleto: { valor: null, observacion: '' },
    identificacionOficial: { valor: null, observacion: '' },
    numeroIdentificacion: { valor: null, observacion: '' },
    numeroEmpleado: { valor: null, observacion: '' },
    fechaHechos: { valor: null, observacion: '' },
    datosDenunciado: { valor: null, observacion: '' },
  };
  readonly totalRequisitos = ORDEN_PREGUNTAS.length;

  /** Observaciones generales, adicionales a las de cada pregunta (opcional). */
  observaciones = '';

  identificaciones: IdentificacionOficialOpcion[] = [];
  tipoIdentificacionPresentada = '';
  cargandoIdentificaciones = false;
  mostrarListadoOficial = false;

  imagenesCredencial: EvidenciaResumen[] = [];
  indiceCredencial = 0;
  credencialUrl: SafeUrl | null = null;
  credencialNombre = '';
  credencialId: number | null = null;
  cargandoCredencial = false;
  private credencialObjectUrl: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private revisionService: RevisionService,
    private toast: ToastService,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.folio = this.route.snapshot.paramMap.get('folio') ?? '';
    this.cargar();
  }

  ngOnDestroy(): void {
    if (this.credencialObjectUrl) {
      URL.revokeObjectURL(this.credencialObjectUrl);
      this.credencialObjectUrl = null;
    }
  }

  // ---------------- lista de verificación ----------------

  /** Acceso tipado desde la plantilla (el ng-template recibe la clave como string). */
  r(clave: string): RespuestaPregunta {
    return this.respuestas[clave as ClavePregunta];
  }

  setRespuesta(clave: string, valor: ValorRespuesta): void {
    this.r(clave).valor = valor;
    this.cdr.detectChanges();
  }

  /**
   * Estado de cada requisito: 'si' (Sí o No aplicable), 'no' o null (sin contestar). El
   * requisito 3 combina la boleta y, si la boleta "No aplica", el número de empleado.
   */
  estadoRequisito(clave: string): 'si' | 'no' | null {
    const valor = this.r(clave).valor;
    if (clave === 'numeroIdentificacion' && valor === 'na') {
      const empleado = this.respuestas.numeroEmpleado.valor;
      return empleado === null ? null : empleado === false ? 'no' : 'si';
    }
    return valor === null ? null : valor === false ? 'no' : 'si';
  }

  get cumplidos(): number {
    return ORDEN_PREGUNTAS.filter((c) => this.estadoRequisito(c) === 'si').length;
  }

  get contestadas(): number {
    return ORDEN_PREGUNTAS.filter((c) => this.estadoRequisito(c) !== null).length;
  }

  /** Preguntas contestadas con "No" (en el requisito 3, la boleta o el número de empleado). */
  get preguntasEnNo(): ClavePregunta[] {
    return ORDEN_PREGUNTAS.filter((c) => this.estadoRequisito(c) === 'no').map((c) =>
      c === 'numeroIdentificacion' && this.respuestas.numeroIdentificacion.valor === 'na' ? 'numeroEmpleado' : c,
    );
  }

  /** Preguntas en "No" a las que todavía les falta la observación para el quejoso. */
  get noSinObservacion(): ClavePregunta[] {
    return this.preguntasEnNo.filter((c) => !this.respuestas[c].observacion.trim());
  }

  get puedeCanalizar(): boolean {
    return this.cumplidos === this.totalRequisitos;
  }

  get puedeRegresar(): boolean {
    return this.preguntasEnNo.length > 0 && this.noSinObservacion.length === 0 && !this.enviando;
  }

  get motivoBloqueoRegresar(): string {
    if (this.preguntasEnNo.length === 0) {
      return 'Marca al menos una pregunta como "No" para regresar la queja.';
    }
    if (this.noSinObservacion.length > 0) {
      return 'Escribe la observación de cada pregunta marcada como "No".';
    }
    return '';
  }

  // ---------------- datos calculados para las preguntas ----------------

  /** Días naturales entre la fecha de los hechos y la fecha de registro de la queja.
   * null si la queja no trae fecha de los hechos (p. ej. registros manuales). */
  get diasDesdeHechos(): number | null {
    const hechos = this.aFechaLocal(this.queja?.fechaHechos);
    const registro = this.aFechaLocal(this.queja?.fechaCreacion);
    if (!hechos || !registro) {
      return null;
    }
    const msPorDia = 24 * 60 * 60 * 1000;
    return Math.round((registro.getTime() - hechos.getTime()) / msPorDia);
  }

  get dentroDePlazo(): boolean | null {
    const dias = this.diasDesdeHechos;
    return dias === null ? null : dias <= this.plazoDias;
  }

  get etiquetaTipoIdentificacion(): string {
    switch ((this.queja?.tipoIdentificacionQuejoso ?? '').toLowerCase()) {
      case 'alumno':
        return 'Número de boleta';
      case 'empleado':
        return 'Número de empleado';
      default:
        return 'Boleta / número de empleado';
    }
  }

  get esEmpleado(): boolean {
    return (this.queja?.tipoIdentificacionQuejoso ?? '').toLowerCase() === 'empleado';
  }

  /** Catálogo de documentos agrupado (identificación oficial, IPN, representación). */
  get gruposIdentificacion(): { grupo: string; items: IdentificacionOficialOpcion[] }[] {
    const grupos: { grupo: string; items: IdentificacionOficialOpcion[] }[] = [];
    for (const doc of this.identificaciones) {
      const nombre = doc.grupo || 'Identificación oficial';
      let g = grupos.find((x) => x.grupo === nombre);
      if (!g) {
        g = { grupo: nombre, items: [] };
        grupos.push(g);
      }
      g.items.push(doc);
    }
    return grupos;
  }

  /** "Boleta" o "Número de empleado" para el dato capturado de la pregunta 3. */
  get etiquetaDatoCapturado(): string {
    return (this.queja?.tipoIdentificacionQuejoso ?? '').toLowerCase() === 'empleado' ? 'Número de empleado' : 'Boleta';
  }

  /** "15/jun/2023" -- formato del cómputo automático de la pregunta 4. */
  fechaCorta(valor: string | null | undefined): string {
    const fecha = this.aFechaLocal(valor);
    if (!fecha) {
      return '';
    }
    const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    return `${String(fecha.getDate()).padStart(2, '0')}/${meses[fecha.getMonth()]}/${fecha.getFullYear()}`;
  }

  get esAlumno(): boolean {
    return (this.queja?.tipoIdentificacionQuejoso ?? '').toLowerCase() === 'alumno';
  }

  /** "2026-09-30" o "2026-09-30T12:34:56" → fecha local a medianoche, sin corrimiento por
   * zona horaria (new Date('2026-09-30') la interpretaría como UTC y en México daría el 29). */
  private aFechaLocal(valor: string | null | undefined): Date | null {
    if (!valor) {
      return null;
    }
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(valor);
    if (!m) {
      return null;
    }
    return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  }

  formatearFecha(valor: string | null | undefined): string {
    const fecha = this.aFechaLocal(valor);
    return fecha
      ? fecha.toLocaleDateString('es-MX', { day: '2-digit', month: 'long', year: 'numeric' })
      : '';
  }

  alternarListadoOficial(): void {
    this.mostrarListadoOficial = !this.mostrarListadoOficial;
  }

  // ---------------- acciones ----------------

  irACanalizar(): void {
    if (!this.puedeCanalizar) {
      this.toast.advertencia('Todos los requisitos deben estar en "Sí" (o "No aplicable") para canalizar la queja.');
      return;
    }
    this.router.navigate(['/turnado', this.folio]);
  }

  regresarAlQuejoso(): void {
    if (!this.puedeRegresar) {
      this.toast.advertencia(this.motivoBloqueoRegresar || 'Revisa las respuestas antes de regresar la queja.');
      return;
    }

    const motivos = this.preguntasEnNo.map((c) => MOTIVO_SI_NO[c]);

    const detalle = this.preguntasEnNo.map(
      (c) => `• ${TITULO_PREGUNTA[c]}: ${this.respuestas[c].observacion.trim()}`,
    );
    const idPresentada = this.identificaciones.find((i) => i.clave === this.tipoIdentificacionPresentada);
    if (idPresentada && this.respuestas.identificacionOficial.valor === false) {
      detalle.push(`• Identificación revisada: ${idPresentada.nombre}`);
    }
    if (this.observaciones.trim()) {
      detalle.push('', this.observaciones.trim());
    }

    this.enviando = true;
    this.revisionService
      .rechazar(this.folio, { motivos, observaciones: detalle.join('\n') })
      .subscribe({
        next: () => {
          this.toast.exito('La queja fue regresada al quejoso con observaciones.');
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.enviando = false;
          this.toast.error(err?.error?.mensaje ?? 'No se pudo regresar la queja.');
          this.cdr.detectChanges();
        },
      });
  }

  verDocumento(id: number): void {
    this.revisionService.descargarEvidencia(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => this.toast.error('No se pudo abrir el documento.'),
    });
  }

  // ---------------- carga ----------------

  soltando = false;

  /** Regresa la queja a la cola (deja de estar a nombre de esta recepcionista). */
  soltar(): void {
    this.soltando = true;
    this.revisionService.liberar(this.folio).subscribe({
      next: () => {
        this.toast.exito(`La queja ${this.folio} regresó a la bandeja.`);
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.soltando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo soltar la queja.');
        this.cdr.detectChanges();
      },
    });
  }

  private cargar(): void {
    this.cargando = true;
    this.revisionService.detalle(this.folio).subscribe({
      next: (queja) => {
        this.queja = queja;
        this.cargando = false;
        this.cargarCredencial();
        this.cargarIdentificaciones();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar la queja.');
        // Si otra recepcionista ya la tiene, no tiene caso quedarse en esta pantalla.
        this.router.navigate(['/']);
      },
    });
  }

  private cargarIdentificaciones(): void {
    this.cargandoIdentificaciones = true;
    this.revisionService.identificacionesOficiales().subscribe({
      next: (lista) => {
        this.identificaciones = lista;
        this.cargandoIdentificaciones = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoIdentificaciones = false;
        this.identificaciones = [];
        this.cdr.detectChanges();
      },
    });
  }

  private cargarCredencial(): void {
    this.imagenesCredencial = (this.queja?.evidencias ?? []).filter((ev) =>
      (ev.tipoMime ?? '').toLowerCase().startsWith('image/'),
    );
    this.indiceCredencial = 0;

    if (this.imagenesCredencial.length === 0) {
      return;
    }

    this.mostrarCredencial(0);
  }

  seleccionarCredencial(indice: number): void {
    if (indice === this.indiceCredencial || indice < 0 || indice >= this.imagenesCredencial.length) {
      return;
    }
    this.mostrarCredencial(indice);
  }

  private mostrarCredencial(indice: number): void {
    const imagen = this.imagenesCredencial[indice];
    if (!imagen) {
      return;
    }

    this.indiceCredencial = indice;
    this.credencialId = imagen.id;
    this.credencialNombre = imagen.nombreArchivo;
    this.cargandoCredencial = true;

    if (this.credencialObjectUrl) {
      URL.revokeObjectURL(this.credencialObjectUrl);
      this.credencialObjectUrl = null;
      this.credencialUrl = null;
    }

    this.revisionService.descargarEvidencia(imagen.id).subscribe({
      next: (blob) => {
        this.credencialObjectUrl = URL.createObjectURL(blob);
        this.credencialUrl = this.sanitizer.bypassSecurityTrustUrl(this.credencialObjectUrl);
        this.cargandoCredencial = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoCredencial = false;
        this.cdr.detectChanges();
      },
    });
  }

  formatearTamanio(bytes: number): string {
    if (!bytes || bytes <= 0) {
      return '';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    const kb = bytes / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(kb < 10 ? 1 : 0)} KB`;
    }
    return `${(kb / 1024).toFixed(1)} MB`;
  }

  tipoIcono(ev: EvidenciaResumen): 'imagen' | 'pdf' | 'archivo' {
    const tipo = (ev.tipoMime ?? '').toLowerCase();
    if (tipo.startsWith('image/')) {
      return 'imagen';
    }
    if (tipo === 'application/pdf') {
      return 'pdf';
    }
    return 'archivo';
  }
}
