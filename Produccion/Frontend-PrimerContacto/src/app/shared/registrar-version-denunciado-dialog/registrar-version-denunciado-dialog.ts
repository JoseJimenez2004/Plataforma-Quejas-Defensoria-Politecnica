import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DenunciadoService } from '../../core/services/denunciado.service';
import { CatalogoDependenciasService, Dependencia, normalizar } from '../../core/services/catalogo-dependencias.service';
import { mensajeDeError } from '../../core/utils/archivos';

export interface RegistrarVersionDenunciadoData {
  folioQueja: string;
  /** Nombre del denunciado según la queja (se separa en nombre y apellidos como sugerencia). */
  denunciado?: string | null;
  /** Unidad de la queja, para preseleccionar la de procedencia. */
  unidadAcademica?: string | null;
}

/* Mismas reglas que denunciado-service (validacion/ReglasDenunciado.java). */
const MB = 1024 * 1024;
const CREDENCIAL_MAX = 2;
const CREDENCIAL_TAMANIO = 3 * MB;
const EVIDENCIA_MAX = 10;
const EVIDENCIA_TAMANIO = 30 * MB;
const EVIDENCIA_TOTAL = 95 * MB;
const DESCRIPCION_MIN = 20;
const DESCRIPCION_MAX = 4000;

/**
 * PC-2 (2026-11): el analista de Primer Contacto registra la versión del denunciado, por
 * ejemplo después de una cita o entrevista. Sustituye al enlace que se le mandaba por correo.
 */
@Component({
  selector: 'app-registrar-version-denunciado-dialog',
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  templateUrl: './registrar-version-denunciado-dialog.html',
  styleUrl: './registrar-version-denunciado-dialog.css'
})
export class RegistrarVersionDenunciadoDialog {
  nombre = '';
  apellido1 = '';
  apellido2 = '';
  unidad = '';
  busquedaUnidad = '';
  tipoIdentificacion: 'ALUMNO' | 'EMPLEADO' = 'EMPLEADO';
  numeroIdentificacion = '';
  fechaEntrevista = '';
  descripcionHechos = '';
  credencial: File[] = [];
  evidencias: File[] = [];

  dependencias: Dependencia[] = [];
  cargandoCatalogo = true;
  guardando = false;
  error = '';

  readonly hoy = new Date().toISOString().slice(0, 10);
  readonly DESCRIPCION_MAX = DESCRIPCION_MAX;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RegistrarVersionDenunciadoData,
    private dialogRef: MatDialogRef<RegistrarVersionDenunciadoDialog>,
    private denunciadoService: DenunciadoService,
    private catalogo: CatalogoDependenciasService,
    private snackBar: MatSnackBar
  ) {
    const partes = (data.denunciado ?? '').trim().split(/\s+/).filter(Boolean);
    if (partes.length >= 3) {
      this.apellido2 = partes.pop()!;
      this.apellido1 = partes.pop()!;
      this.nombre = partes.join(' ');
    } else if (partes.length === 2) {
      [this.nombre, this.apellido1] = partes;
    } else if (partes.length === 1) {
      this.nombre = partes[0];
    }

    this.catalogo.listar().subscribe(lista => {
      this.dependencias = [...lista].sort((a, b) => (a.abreviatura || a.nombre).localeCompare(b.abreviatura || b.nombre, 'es'));
      this.cargandoCatalogo = false;
      const propia = this.catalogo.buscar(lista, data.unidadAcademica);
      if (propia) this.unidad = propia.clave;
    });
  }

  get dependenciasFiltradas(): Dependencia[] {
    const q = normalizar(this.busquedaUnidad);
    if (!q) return this.dependencias;
    return this.dependencias.filter(d => d.clave === this.unidad
      || [d.abreviatura, d.clave, d.nombre].some(v => normalizar(v).includes(q)));
  }

  etiquetaDependencia(d: Dependencia): string {
    return d.abreviatura && d.abreviatura !== d.nombre ? `${d.abreviatura} · ${d.nombre}` : d.nombre;
  }

  elegirCredencial(evento: Event): void {
    this.credencial = this.agregar(this.credencial, evento, CREDENCIAL_MAX);
  }

  elegirEvidencias(evento: Event): void {
    this.evidencias = this.agregar(this.evidencias, evento, EVIDENCIA_MAX);
  }

  quitar(lista: 'credencial' | 'evidencias', i: number): void {
    this[lista] = this[lista].filter((_, j) => j !== i);
  }

  private agregar(actuales: File[], evento: Event, maximo: number): File[] {
    const input = evento.target as HTMLInputElement;
    const nuevos = Array.from(input.files ?? []);
    input.value = '';
    return [...actuales, ...nuevos].slice(0, maximo);
  }

  tamanio(bytes: number): string {
    return bytes >= MB ? (bytes / MB).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
  }

  /** Validación previa (el servidor vuelve a validar todo). */
  private validar(): string {
    if (!this.nombre.trim() || !this.apellido1.trim()) return 'Escribe el nombre y el primer apellido del denunciado.';
    if (!this.unidad) return 'Selecciona la unidad de procedencia.';
    if (!/^\d{1,10}$/.test(this.numeroIdentificacion.trim())) {
      return this.tipoIdentificacion === 'ALUMNO'
        ? 'El número de boleta solo admite dígitos (máximo 10).'
        : 'El número de empleado solo admite dígitos (máximo 10).';
    }
    if (this.fechaEntrevista && this.fechaEntrevista > this.hoy) return 'La fecha de la entrevista no puede ser futura.';
    const d = this.descripcionHechos.trim();
    if (d.length < DESCRIPCION_MIN) return `Escribe la versión de los hechos (al menos ${DESCRIPCION_MIN} caracteres).`;
    if (d.length > DESCRIPCION_MAX) return `La versión no puede exceder ${DESCRIPCION_MAX} caracteres.`;
    for (const f of this.credencial) {
      if (!/^image\/(jpeg|png)$/.test(f.type)) return `La credencial debe ser JPG o PNG ("${f.name}" no lo es).`;
      if (f.size > CREDENCIAL_TAMANIO) return `La imagen "${f.name}" pesa más de 3 MB.`;
    }
    let total = 0;
    for (const f of this.evidencias) {
      if (f.size > EVIDENCIA_TAMANIO) return `La evidencia "${f.name}" pesa más de 30 MB.`;
      total += f.size;
    }
    if (total > EVIDENCIA_TOTAL) return 'Las evidencias suman más de 95 MB.';
    return '';
  }

  guardar(): void {
    this.error = this.validar();
    if (this.error) return;

    this.guardando = true;
    this.denunciadoService.registrarVersion({
      folioQueja: this.data.folioQueja,
      nombre: this.nombre.trim(),
      apellido1: this.apellido1.trim(),
      apellido2: this.apellido2.trim() || undefined,
      unidadProcedenciaClave: this.unidad,
      tipoIdentificacion: this.tipoIdentificacion,
      numeroIdentificacion: this.numeroIdentificacion.trim(),
      fechaEntrevista: this.fechaEntrevista || undefined,
      descripcionHechos: this.descripcionHechos.trim(),
      credencial: this.credencial,
      evidencias: this.evidencias
    }).subscribe({
      next: (r) => {
        this.guardando = false;
        this.snackBar.open(`Versión del denunciado registrada (${r?.folioRespuesta ?? 'sin folio'}).`, 'Cerrar', { duration: 3500 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.guardando = false;
        this.error = mensajeDeError(err, 'No fue posible registrar la versión del denunciado.');
      }
    });
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }
}
