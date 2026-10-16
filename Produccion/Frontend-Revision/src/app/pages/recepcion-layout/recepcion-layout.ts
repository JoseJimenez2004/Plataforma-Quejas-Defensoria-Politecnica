import { ChangeDetectorRef, Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthRevisionService } from '../../core/services/auth-revision.service';
import { PerfilService } from '../../core/services/perfil.service';
import { RevisionService } from '../../core/services/revision.service';
import { ToastService } from '../../core/services/toast.service';
import { etiquetaRol, iniciales } from '../../core/models/revision.models';
import { CampanaNotificaciones } from '../../shared/campana-notificaciones/campana-notificaciones';

/** Cada cuánto se avisa al backend que la queja abierta sigue en uso. Debe ser menor que el
 * límite de inactividad de revision-service (20 min) para que no se libere a media revisión. */
const INTERVALO_RENOVAR_MS = 4 * 60 * 1000;

/** Pantallas que trabajan una queja tomada: /validacion/FOL-…, /turnado/FOL-…, /rechazo/FOL-… */
const RUTA_CON_QUEJA = /^\/(validacion|turnado|rechazo)\/([^/?#]+)/;

@Component({
  selector: 'app-recepcion-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet, CampanaNotificaciones],
  templateUrl: './recepcion-layout.html',
  styleUrl: './recepcion-layout.scss',
})
export class RecepcionLayout implements OnInit {
  readonly etiquetaRol = etiquetaRol;
  readonly anio = new Date().getFullYear();

  private readonly revisionService = inject(RevisionService);
  private readonly destroyRef = inject(DestroyRef);
  private renovarId: ReturnType<typeof setInterval> | null = null;
  private folioRenovando = '';

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
    this.vigilarQuejaAbierta(this.router.url);
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe((e) => this.vigilarQuejaAbierta((e as NavigationEnd).urlAfterRedirects));
    this.destroyRef.onDestroy(() => this.detenerRenovacion());

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

  /**
   * Mientras la recepcionista tenga abierta una queja (validación, rechazo o turnado), se le
   * avisa al backend cada pocos minutos que la sigue trabajando; así no se libera sola en una
   * revisión larga. Al salir de esas pantallas se deja de avisar y, si la abandona sin
   * terminar, el backend la libera a los 20 minutos.
   */
  private vigilarQuejaAbierta(url: string): void {
    const folio = RUTA_CON_QUEJA.exec(url)?.[2] ?? '';
    if (folio === this.folioRenovando) {
      return;
    }
    this.detenerRenovacion();
    if (!folio) {
      return;
    }
    this.folioRenovando = folio;
    this.renovarId = setInterval(() => {
      this.revisionService.renovar(folio).subscribe({
        error: (err) => {
          this.detenerRenovacion();
          this.toast.advertencia(err?.error?.mensaje ?? 'Esta queja ya no está a tu nombre.');
        },
      });
    }, INTERVALO_RENOVAR_MS);
  }

  private detenerRenovacion(): void {
    if (this.renovarId !== null) {
      clearInterval(this.renovarId);
    }
    this.renovarId = null;
    this.folioRenovando = '';
  }
}
