import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InstanciaRemision, InstanciasRemisionService } from '../../core/services/instancias-remision.service';
import { ToastService } from '../../core/services/toast.service';

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Correos de las instancias a las que Primer Contacto remite los expedientes improcedentes.
 * El oficio se manda a estos correos en cuanto el analista lo genera.
 */
@Component({
  selector: 'app-instancias-remision',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './instancias-remision.html',
  styleUrl: './instancias-remision.scss',
})
export class InstanciasRemision implements OnInit {
  lista: InstanciaRemision[] = [];
  cargando = true;
  error = false;

  editando: string | null = null;
  correoEditado = '';
  guardando = false;

  constructor(
    private service: InstanciasRemisionService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = false;
    this.service.listar().subscribe({
      next: (lista) => {
        this.lista = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
      },
    });
  }

  editar(instancia: InstanciaRemision): void {
    this.editando = instancia.clave;
    this.correoEditado = instancia.correo ?? '';
  }

  cancelar(): void {
    this.editando = null;
    this.correoEditado = '';
  }

  get correoValido(): boolean {
    return CORREO.test(this.correoEditado.trim());
  }

  guardar(instancia: InstanciaRemision): void {
    const correo = this.correoEditado.trim();
    if (!CORREO.test(correo)) {
      this.toast.advertencia('Escribe un correo válido.');
      return;
    }
    this.guardando = true;
    this.service.cambiarCorreo(instancia.clave, correo).subscribe({
      next: (actualizada) => {
        this.lista = this.lista.map((i) => (i.clave === actualizada.clave ? actualizada : i));
        this.guardando = false;
        this.editando = null;
        this.toast.exito(`Correo de ${actualizada.sigla} actualizado.`);
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardando = false;
        this.toast.error('No se pudo guardar el correo.');
        this.cdr.detectChanges();
      },
    });
  }

  fecha(valor?: string): string {
    if (!valor) return '—';
    const d = new Date(valor);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleString('es-MX', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
