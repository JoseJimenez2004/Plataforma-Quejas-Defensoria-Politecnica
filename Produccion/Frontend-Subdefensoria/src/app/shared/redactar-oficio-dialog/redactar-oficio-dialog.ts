import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OficioService } from '../../core/services/oficio.service';
import { Oficio } from '../../core/models/oficio';
import { CatalogoDependenciasService, Dependencia, normalizar } from '../../core/services/catalogo-dependencias.service';

export interface RedactarOficioDialogData {
  expedienteId: number;
  folio: string;
  siguienteFase: 'SOLICITUD_INFORMACION' | 'GESTION_DIRECTOR';
  unidadAcademica: string;
}

@Component({
  selector: 'app-redactar-oficio-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './redactar-oficio-dialog.html',
  styleUrl: './redactar-oficio-dialog.css'
})
export class RedactarOficioDialog {
  destinatarioNombre = '';
  destinatarioCorreo = '';
  unidadAcademica: string;
  contenidoRedactado = '';
  enviando = false;

  // ---- SD-1: unidad del catálogo (titular y correo)
  dependencias: Dependencia[] = [];
  claveDependencia = '';
  busquedaUnidad = '';
  cargandoCatalogo = true;
  sinCorreo = false;

  /** Un solo tipo de oficio: la solicitud de información, que responde el director. */
  readonly tituloAccion = 'Solicitud de Información';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RedactarOficioDialogData,
    private dialogRef: MatDialogRef<RedactarOficioDialog>,
    private oficioService: OficioService,
    private snackBar: MatSnackBar,
    private catalogo: CatalogoDependenciasService
  ) {
    this.unidadAcademica = data.unidadAcademica ?? '';
    this.catalogo.listar().subscribe(lista => {
      this.dependencias = [...lista].sort((a, b) => (a.abreviatura || a.nombre).localeCompare(b.abreviatura || b.nombre, 'es'));
      this.cargandoCatalogo = false;
      const propia = this.catalogo.buscar(lista, this.unidadAcademica);
      if (propia) this.elegirDependencia(propia.clave);
    });
  }

  /** Lista filtrada por lo que se escribe en "Buscar unidad". La elegida siempre se queda. */
  get dependenciasFiltradas(): Dependencia[] {
    const q = normalizar(this.busquedaUnidad);
    if (!q) return this.dependencias;
    return this.dependencias.filter(d => d.clave === this.claveDependencia
      || [d.abreviatura, d.clave, d.nombre, d.nombreTitular].some(v => normalizar(v).includes(q)));
  }

  etiquetaDependencia(d: Dependencia): string {
    return d.abreviatura && d.abreviatura !== d.nombre ? `${d.abreviatura} · ${d.nombre}` : d.nombre;
  }

  /** Al elegir la unidad se llenan titular y correo (los dos siguen siendo editables). */
  elegirDependencia(clave: string): void {
    this.claveDependencia = clave;
    const d = this.dependencias.find(x => x.clave === clave);
    if (!d) return;
    this.unidadAcademica = d.abreviatura || d.nombre;
    this.destinatarioNombre = d.nombreTitular ?? '';
    this.destinatarioCorreo = d.correoContacto ?? '';
    this.sinCorreo = !d.correoContacto;
  }

  cancelar(): void {
    this.dialogRef.close();
  }

  enviar(): void {
    if (!this.destinatarioNombre.trim() || !this.destinatarioCorreo.trim() || !this.contenidoRedactado.trim()) {
      this.snackBar.open('Completa destinatario, correo y contenido del oficio.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.enviando = true;

    this.oficioService.crearOficio({
      expedienteId: this.data.expedienteId,
      destinatarioNombre: this.destinatarioNombre.trim(),
      destinatarioCorreo: this.destinatarioCorreo.trim(),
      unidadAcademica: this.unidadAcademica.trim(),
      contenidoRedactado: this.contenidoRedactado.trim()
    }).subscribe({
      next: (oficio: Oficio) => {
        this.dialogRef.close(oficio);
      },
      error: (err) => {
        this.enviando = false;
        const mensaje = err?.error?.error ?? 'No fue posible generar el oficio.';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }
}
