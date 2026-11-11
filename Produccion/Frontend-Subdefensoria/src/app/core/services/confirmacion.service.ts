import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

import { ConfirmarDialog, ConfirmarDialogData } from '../../shared/confirmar-dialog/confirmar-dialog';

/**
 * Reemplaza al confirm() del navegador con el aviso del sistema.
 *   if (!(await this.confirmacion.confirmar({ titulo: '¿...?' }))) return;
 * Resuelve true solo si se pulsa el botón de confirmar (Esc o clic fuera = false).
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  constructor(private dialog: MatDialog) {}

  confirmar(data: ConfirmarDialogData): Promise<boolean> {
    const ref = this.dialog.open<ConfirmarDialog, ConfirmarDialogData, boolean>(ConfirmarDialog, {
      data,
      width: '440px',
      maxWidth: 'calc(100vw - 32px)',
      autoFocus: 'dialog',
      backdropClass: 'confirmar-fondo',
      panelClass: 'confirmar-panel'
    });
    return firstValueFrom(ref.afterClosed()).then(v => v === true);
  }
}
