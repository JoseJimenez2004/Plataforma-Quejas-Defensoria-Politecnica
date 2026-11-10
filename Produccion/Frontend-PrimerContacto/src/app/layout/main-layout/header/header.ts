import { ChangeDetectorRef, Component, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { SesionPersonalService } from '../../../core/services/sesion-personal.service';
import { PerfilService } from '../../../core/services/perfil.service';

/** Encabezado con menú de perfil y cambio de contraseña, igual que Recepción. */
@Component({
  selector: 'app-header',
  imports: [FormsModule, MatSnackBarModule],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class Header {

  menuAbierto = false;
  mostrarModalPassword = false;

  passwordActual = '';
  passwordNueva = '';
  passwordConfirmar = '';
  mostrarActual = false;
  mostrarNueva = false;
  guardandoPassword = false;
  errorPassword = '';

  constructor(
    private sesion: SesionPersonalService,
    private perfilService: PerfilService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  get nombreUsuario(): string {
    return this.sesion.getNombre() ?? 'Primer Contacto';
  }

  /** Iniciales para el avatar ("María López" → "ML"). */
  get iniciales(): string {
    const partes = this.nombreUsuario.split(/\s+/).filter((p) => p && !p.endsWith('.'));
    return ((partes[0]?.charAt(0) ?? '') + (partes[1]?.charAt(0) ?? '')).toUpperCase() || '?';
  }

  @HostListener('document:click')
  cerrarMenuAlHacerClickFuera(): void {
    this.menuAbierto = false;
  }

  alternarMenu(evento: Event): void {
    evento.stopPropagation();
    this.menuAbierto = !this.menuAbierto;
  }

  abrirModalPassword(): void {
    this.menuAbierto = false;
    this.passwordActual = '';
    this.passwordNueva = '';
    this.passwordConfirmar = '';
    this.errorPassword = '';
    this.mostrarModalPassword = true;
  }

  cerrarModalPassword(): void {
    this.mostrarModalPassword = false;
  }

  get cumpleLongitud(): boolean {
    return this.passwordNueva.length >= 8;
  }

  get cumpleMayuscula(): boolean {
    return /[A-Z]/.test(this.passwordNueva);
  }

  get cumpleNumero(): boolean {
    return /\d/.test(this.passwordNueva);
  }

  guardarPassword(): void {
    this.errorPassword = '';
    if (!this.passwordActual) {
      this.errorPassword = 'Ingresa tu contraseña actual.';
      return;
    }
    if (!this.cumpleLongitud || !this.cumpleMayuscula || !this.cumpleNumero) {
      this.errorPassword = 'La nueva contraseña no cumple los requisitos.';
      return;
    }
    if (this.passwordNueva !== this.passwordConfirmar) {
      this.errorPassword = 'La confirmación no coincide con la nueva contraseña.';
      return;
    }

    this.guardandoPassword = true;
    this.perfilService.cambiarPassword({
      passwordActual: this.passwordActual,
      passwordNueva: this.passwordNueva
    }).subscribe({
      next: () => {
        this.guardandoPassword = false;
        this.mostrarModalPassword = false;
        this.snackBar.open('Contraseña actualizada correctamente.', 'Cerrar', { duration: 3000 });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardandoPassword = false;
        this.errorPassword = err?.error?.mensaje ?? 'No se pudo actualizar la contraseña.';
        this.cdr.detectChanges();
      }
    });
  }

  cerrarSesion(): void {
    this.sesion.cerrarSesion();
    window.location.assign('/revision/login');
  }
}
