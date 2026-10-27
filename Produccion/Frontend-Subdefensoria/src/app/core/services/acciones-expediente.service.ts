import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Observable, map, of } from 'rxjs';

import { ExpedienteResumen } from '../models/expediente-resumen';
import { Accion } from '../utils/etapas';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';
import { RedactarOficioDialog, RedactarOficioDialogData } from '../../shared/redactar-oficio-dialog/redactar-oficio-dialog';
import { RespuestaExternaDialog, RespuestaExternaDialogData } from '../../shared/respuesta-externa-dialog/respuesta-externa-dialog';

/**
 * Ejecuta el "siguiente paso" de un expediente desde Inicio, Mis expedientes o Plazos:
 * abre el diálogo que toca (oficio, recordatorio, respuesta) o lleva al expediente.
 * Devuelve true cuando hubo un cambio y la pantalla debe recargar.
 */
@Injectable({ providedIn: 'root' })
export class AccionesExpedienteService {

  constructor(
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ejecutar(e: ExpedienteResumen, accion: Accion): Observable<boolean> {
    switch (accion) {
      case 'oficio': return this.oficio(e);
      case 'recordatorio': return this.recordatorio(e);
      case 'respuesta': return this.respuesta(e);
      case 'acuerdos':
        this.router.navigate(['/expediente', e.folio], { queryParams: { tab: 'acuerdos' } });
        return of(false);
      default:
        this.abrir(e);
        return of(false);
    }
  }

  abrir(e: ExpedienteResumen): void {
    this.router.navigate(['/expediente', e.folio]);
  }

  private oficio(e: ExpedienteResumen): Observable<boolean> {
    if (!e.siguienteFase) { this.abrir(e); return of(false); }
    const data: RedactarOficioDialogData = {
      expedienteId: e.expedienteId,
      folio: e.folio,
      siguienteFase: e.siguienteFase,
      unidadAcademica: e.unidadAcademica
    };
    return this.dialog.open(RedactarOficioDialog, { width: '560px', data }).afterClosed()
      .pipe(map(r => this.avisar(!!r, 'Oficio generado correctamente.')));
  }

  private recordatorio(e: ExpedienteResumen): Observable<boolean> {
    if (!e.oficioIdVigente || !e.numeroOficioVigente || !e.faseOficioVigente) { this.abrir(e); return of(false); }
    const data: RecordatorioDialogData = {
      oficioId: e.oficioIdVigente,
      numeroOficio: e.numeroOficioVigente,
      fase: e.faseOficioVigente
    };
    return this.dialog.open(RecordatorioDialog, { width: '520px', data }).afterClosed()
      .pipe(map(r => this.avisar(!!r, 'Recordatorio registrado correctamente.')));
  }

  private respuesta(e: ExpedienteResumen): Observable<boolean> {
    if (!e.oficioIdVigente || !e.numeroOficioVigente) { this.abrir(e); return of(false); }
    const data: RespuestaExternaDialogData = {
      oficioId: e.oficioIdVigente,
      numeroOficio: e.numeroOficioVigente,
      destinatarioNombre: e.destinatarioNombreVigente ?? e.unidadAcademica
    };
    return this.dialog.open(RespuestaExternaDialog, { width: '560px', data }).afterClosed()
      .pipe(map(r => this.avisar(!!r, 'Respuesta registrada correctamente.')));
  }

  private avisar(hubo: boolean, mensaje: string): boolean {
    if (hubo) this.snackBar.open(mensaje, 'Cerrar', { duration: 3000 });
    return hubo;
  }
}
