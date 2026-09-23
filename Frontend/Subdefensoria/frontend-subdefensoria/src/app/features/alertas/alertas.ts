import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AlertaService } from '../../core/services/alerta.service';
import { AlertaVencimiento } from '../../core/models/alerta-vencimiento';
import { RecordatorioDialog, RecordatorioDialogData } from '../../shared/recordatorio-dialog/recordatorio-dialog';

@Component({
  selector: 'app-alertas',
  imports: [MatCardModule, MatTableModule, MatButtonModule, MatIconModule, MatTooltipModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './alertas.html',
  styleUrl: './alertas.css'
})
export class Alertas implements OnInit {
  displayedColumns = ['folio', 'unidadAcademica', 'fechaLimite', 'diasRetraso', 'accion'];
  vencidos: AlertaVencimiento[] = [];

  constructor(
    private alertaService: AlertaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.alertaService.obtenerVencidos().subscribe({
      next: (items) => {
        this.vencidos = items;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
        this.snackBar.open('No fue posible cargar el panel de alertas.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  atender(alerta: AlertaVencimiento): void {
    const data: RecordatorioDialogData = {
      oficioId: alerta.oficioId,
      numeroOficio: alerta.numeroOficio,
      fase: alerta.fase
    };

    this.dialog.open(RecordatorioDialog, { width: '520px', data }).afterClosed().subscribe(resultado => {
      if (resultado) {
        this.snackBar.open('Recordatorio enviado correctamente.', 'Cerrar', { duration: 3000 });
        this.cargar();
      }
    });
  }
}
