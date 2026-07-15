import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { SeguridadService } from '../../core/services/seguridad.service';
import { ToastService } from '../../core/services/toast.service';
import { BitacoraAccion, RespaldoResumen } from '../../core/models/admin.models';

@Component({
  selector: 'app-seguridad-respaldos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seguridad-respaldos.html',
  styleUrl: './seguridad-respaldos.scss',
})
export class SeguridadRespaldos implements OnInit {
  respaldos: RespaldoResumen[] = [];
  bitacora: BitacoraAccion[] = [];
  cargando = true;
  ejecutandoRespaldo = false;

  mostrarModalRestaurar = false;
  archivoARestaurar = '';
  confirmacionTexto = '';

  constructor(
    private seguridadService: SeguridadService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.cargando = true;
    this.seguridadService.listarRespaldos().subscribe({
      next: (respaldos) => {
        this.respaldos = respaldos;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
    this.seguridadService.bitacora().subscribe({
      next: (bitacora) => {
        this.bitacora = bitacora;
        this.cdr.detectChanges();
      },
    });
  }

  get ultimoRespaldoTexto(): string {
    return this.respaldos.length ? this.respaldos[0].fecha : 'Sin respaldos todavía';
  }

  ejecutarRespaldoManual(): void {
    this.ejecutandoRespaldo = true;
    this.seguridadService.respaldoManual().subscribe({
      next: () => {
        this.ejecutandoRespaldo = false;
        this.toast.exito('Respaldo manual generado correctamente.');
        this.cargarTodo();
      },
      error: (err) => {
        this.ejecutandoRespaldo = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo generar el respaldo.');
        this.cdr.detectChanges();
      },
    });
  }

  descargar(respaldo: RespaldoResumen): void {
    window.open(this.seguridadService.urlDescarga(respaldo.nombreArchivo), '_blank');
  }

  abrirRestaurar(respaldo: RespaldoResumen): void {
    this.archivoARestaurar = respaldo.nombreArchivo;
    this.confirmacionTexto = '';
    this.mostrarModalRestaurar = true;
  }

  cerrarRestaurar(): void {
    this.mostrarModalRestaurar = false;
  }

  confirmarRestauracion(): void {
    if (this.confirmacionTexto !== 'RESTAURAR') {
      this.toast.advertencia('Escribe RESTAURAR (en mayúsculas) para confirmar esta acción.');
      return;
    }
    this.seguridadService.restaurar(this.archivoARestaurar).subscribe({
      next: () => {
        this.mostrarModalRestaurar = false;
        this.toast.exito('Base de datos restaurada correctamente.');
      },
      error: (err) => {
        this.toast.error(err?.error?.mensaje ?? 'No se pudo restaurar el respaldo.');
        this.cdr.detectChanges();
      },
    });
  }

  formatearTamanio(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
