import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { PerfilService } from '../../../core/services/perfil.service';
import { AuthService } from '../../../core/services/auth.service';
import { UsuarioPerfilDTO } from '../../../models/auth.models';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './perfil.component.html',
})
export class PerfilComponent implements OnInit {
  private perfilService = inject(PerfilService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  perfil: UsuarioPerfilDTO | null = null;
  cargando = true;
  guardando = false;
  exito = '';
  error = '';

  form = this.fb.group({
    correoPersonal: [''],
    telefonoCelular: [''],
    nombreTutor: [''],
    parentescoTutor: [''],
    telefonoTutor: [''],
  });

  ngOnInit(): void {
    this.perfilService.obtener().subscribe({
      next: (p) => {
        this.perfil = p;
        this.form.patchValue({
          correoPersonal: p.correoPersonal,
          telefonoCelular: p.telefonoCelular,
          nombreTutor: p.nombreTutor,
          parentescoTutor: p.parentescoTutor,
          telefonoTutor: p.telefonoTutor,
        });
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  guardar(): void {
    if (!this.perfil) return;
    this.guardando = true;
    this.exito = '';
    this.error = '';

    const updated: UsuarioPerfilDTO = {
      ...this.perfil,
      ...this.form.value,
    } as UsuarioPerfilDTO;

    this.perfilService.actualizar(updated).subscribe({
      next: () => {
        this.exito = 'Perfil actualizado correctamente.';
        localStorage.setItem('ddp_perfil', JSON.stringify(updated));
        this.guardando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err.error?.mensaje ?? 'Error al guardar los cambios.';
        this.guardando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
