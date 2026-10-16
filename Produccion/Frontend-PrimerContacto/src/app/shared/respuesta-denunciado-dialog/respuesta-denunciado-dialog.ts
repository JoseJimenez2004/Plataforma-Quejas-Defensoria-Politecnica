import { Component, Inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ArchivoRespuestaDenunciado, RespuestaDenunciado } from '../../core/models/denunciado.model';
import { DenunciadoService } from '../../core/services/denunciado.service';
import { abrirBlob, mensajeDeError } from '../../core/utils/archivos';

export interface RespuestaDenunciadoDialogData {
  folioQueja: string;
  /** De la más reciente a la más antigua. */
  respuestas: RespuestaDenunciado[];
}

/** Lo que contestó el denunciado en el formulario del portal (denunciado-service). */
@Component({
  selector: 'app-respuesta-denunciado-dialog',
  imports: [DatePipe, MatDialogModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  templateUrl: './respuesta-denunciado-dialog.html',
  styleUrl: './respuesta-denunciado-dialog.css'
})
export class RespuestaDenunciadoDialog {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: RespuestaDenunciadoDialogData,
    private denunciadoService: DenunciadoService,
    private snackBar: MatSnackBar
  ) {}

  nombreCompleto(r: RespuestaDenunciado): string {
    return [r.nombre, r.apellido1, r.apellido2].filter(p => !!p).join(' ');
  }

  identificacion(r: RespuestaDenunciado): string {
    const tipo = r.tipoIdentificacion === 'EMPLEADO' ? 'Núm. de empleado' : 'Boleta';
    return `${tipo}: ${r.numeroIdentificacion}`;
  }

  etiquetaArchivo(a: ArchivoRespuestaDenunciado): string {
    return a.tipo === 'IDENTIFICACION' ? 'Credencial' : 'Evidencia';
  }

  tamanio(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
      : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  abrir(a: ArchivoRespuestaDenunciado): void {
    this.denunciadoService.descargarArchivo(a.id).subscribe({
      next: (archivo) => abrirBlob(archivo),
      error: (error) => {
        this.snackBar.open(mensajeDeError(error, 'No fue posible abrir el archivo.'), 'Cerrar', {
          duration: 3500
        });
      }
    });
  }
}
