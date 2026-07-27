import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthRevisionService } from '../../core/services/auth-revision.service';
import { PerfilService } from '../../core/services/perfil.service';
import { ToastService } from '../../core/services/toast.service';
import { etiquetaRol, iniciales } from '../../core/models/revision.models';

@Component({
  selector: 'app-recepcion-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './recepcion-layout.html',
  styleUrl: './recepcion-layout.scss',
})
export class RecepcionLayout implements OnInit {
  readonly etiquetaRol = etiquetaRol;

  menuAbierto = false;
  mostrarModalPassword = false;
  cambioObligatorio = false;

  passwordActual = '';
  passwordNueva = '';
  passwordConfirmar = '';
  mostrarActual = false;
  mostrarNueva = false;
  guardandoPassword = false;
  errorPassword = '';

  constructor(
    public authService: AuthRevisionService,
    private perfilService: PerfilService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const usuario = this.authService.usuarioActual();
    if (usuario?.forzarCambioPassword) {
      this.cambioObligatorio = true;
      this.abrirModalPassword();
    }
  }

  get nombreUsuario(): string {
    return this.authService.usuarioActual()?.nombre ?? '';
  }

  get rolUsuario(): string {
    const rol = this.authService.usuarioActual()?.rol;
    return rol ? this.etiquetaRol(rol) : '';
  }

  get inicialesUsuario(): string {
    return iniciales(this.nombreUsuario);
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
    if (this.cambioObligatorio) {
      this.toast.advertencia('Debes actualizar tu contraseña temporal antes de continuar.');
      return;
    }
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

  get cumpleCoinciden(): boolean {
    return this.passwordNueva.length > 0 && this.passwordNueva === this.passwordConfirmar;
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
    if (!this.cumpleCoinciden) {
      this.errorPassword = 'La confirmación no coincide con la nueva contraseña.';
      return;
    }

    this.guardandoPassword = true;
    this.perfilService.cambiarPassword({
      passwordActual: this.passwordActual,
      passwordNueva: this.passwordNueva,
    }).subscribe({
      next: () => {
        this.guardandoPassword = false;
        this.cambioObligatorio = false;
        this.mostrarModalPassword = false;
        this.authService.marcarPasswordActualizada();
        this.toast.exito('Contraseña actualizada correctamente.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardandoPassword = false;
        this.errorPassword = err?.error?.mensaje ?? 'No se pudo actualizar la contraseña.';
        this.cdr.detectChanges();
      },
    });
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
