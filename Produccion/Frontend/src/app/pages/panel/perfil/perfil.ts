import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { PerfilUsuario } from '../../../core/models/auth.models';
import { Dependencia } from '../../../core/models/catalogo.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  perfil: PerfilUsuario | null = null;
  dependencias: Dependencia[] = [];
  cargando = true;
  guardando = false;

  // Copia de trabajo de los campos editables.
  correoPersonal = '';
  telefonoCelular = '';
  unidadAcademica = '';
  domicilio = '';

  constructor(
    public authService: AuthService,
    private catalogoService: CatalogoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.authService.obtenerPerfil().subscribe({
      next: (perfil) => {
        this.perfil = perfil;
        this.correoPersonal = perfil.correoPersonal ?? '';
        this.telefonoCelular = perfil.telefonoCelular ?? '';
        this.unidadAcademica = perfil.unidadAcademica ?? '';
        this.domicilio = perfil.domicilio ?? '';
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo cargar tu perfil.');
        this.cdr.detectChanges();
      },
    });

    this.catalogoService.listarDependencias().subscribe({
      next: (dependencias) => {
        this.dependencias = dependencias;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  guardarCambios(): void {
    if (this.telefonoCelular && !/^\d{10}$/.test(this.telefonoCelular)) {
      this.toast.advertencia('El teléfono celular debe tener 10 dígitos.');
      return;
    }

    this.guardando = true;
    this.authService
      .actualizarPerfil({
        correoPersonal: this.correoPersonal || undefined,
        telefonoCelular: this.telefonoCelular || undefined,
        unidadAcademica: this.unidadAcademica || undefined,
        domicilio: this.domicilio || undefined,
      })
      .subscribe({
        next: (perfil) => {
          this.guardando = false;
          this.perfil = perfil;
          this.toast.exito('Tu perfil se actualizó correctamente.');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.guardando = false;
          this.toast.error(err?.error?.mensaje ?? 'No se pudo guardar tu perfil. Intenta de nuevo.');
          this.cdr.detectChanges();
        },
      });
  }
}
