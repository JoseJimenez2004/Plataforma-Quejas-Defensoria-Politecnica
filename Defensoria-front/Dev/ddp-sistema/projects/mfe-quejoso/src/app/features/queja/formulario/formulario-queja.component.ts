import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators, FormGroup } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../../../components/header/header.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { QuejaService } from '../../../core/services/queja.service';
import { TutorDTO } from '../../../models/queja.models';

const UNIDADES = ['ESCOM', 'ENCB', 'ESIQIE', 'ESIA', 'UPIICSA', 'CECYT', 'OTRA'];
const PARENTESCOS = ['Padre', 'Madre', 'Tutor Legal', 'Abuelo/a', 'Tío/a', 'Otro'];

function fechaHechosValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const fecha = new Date(control.value + 'T00:00:00');
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const limite90 = new Date();
  limite90.setDate(limite90.getDate() - 90);
  limite90.setHours(0, 0, 0, 0);
  if (fecha > hoy) return { futuro: true };
  if (fecha < limite90) return { muyAntigua: true };
  return null;
}

@Component({
  selector: 'app-formulario-queja',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule, HeaderComponent, FooterComponent],
  templateUrl: './formulario-queja.component.html',
})
/**
 * Componente público para el registro de una nueva queja.
 * Permite a cualquier usuario enviar una queja que será evaluada por la defensoría.
 */
export class FormularioQuejaComponent {
  private fb = inject(FormBuilder);
  private quejaService = inject(QuejaService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  unidades = UNIDADES;
  parentescos = PARENTESCOS;

  form = this.fb.group({
    nombreQuejoso: ['', Validators.required],
    primerApellido: ['', Validators.required],
    segundoApellido: [''],
    correo: ['', [Validators.required, Validators.email]],
    fechaNacimiento: ['', Validators.required],
    identificacionInstitucional: ['', Validators.required],
    tipoIdentificacion: ['ALUMNO', Validators.required],
    unidadAcademica: ['', Validators.required],
    fechaHechos: ['', [Validators.required, fechaHechosValidator]],
    nombreDenunciado: ['', Validators.required],
    primerApellidoDenunciado: ['', Validators.required],
    segundoApellidoDenunciado: [''],
    descripcionHechos: ['', [Validators.required, Validators.minLength(30)]],
  });

  tutorForm: FormGroup = this.fb.group({
    nombre: ['', Validators.required],
    primerApellido: ['', Validators.required],
    segundoApellido: [''],
    parentesco: ['Padre', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    telefono: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
  });

  archivos: File[] = [];
  tutorData: TutorDTO | null = null;
  mostrarModalTutor = false;
  esMenor = false;
  cargando = false;
  error = '';
  currentYear = new Date().getFullYear();

  get hoy(): string {
    return new Date().toISOString().split('T')[0];
  }

  verificarEdad(): void {
    const fechaStr = this.form.get('fechaNacimiento')?.value;
    if (!fechaStr) return;

    const nacimiento = new Date(fechaStr);
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const m = hoy.getMonth() - nacimiento.getMonth();
    if (m < 0 || (m === 0 && hoy.getDate() < nacimiento.getDate())) edad--;

    this.esMenor = edad < 18;
    if (this.esMenor) {
      this.mostrarModalTutor = true;
    } else {
      this.tutorData = null;
    }
  }

  confirmarTutor(): void {
    if (this.tutorForm.invalid) {
      this.tutorForm.markAllAsTouched();
      return;
    }
    this.tutorData = this.tutorForm.value as TutorDTO;
    this.mostrarModalTutor = false;
  }

  cancelarTutor(): void {
    this.mostrarModalTutor = false;
    if (!this.tutorData) {
      this.form.get('fechaNacimiento')?.setValue('');
      this.esMenor = false;
    }
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      const nuevos = Array.from(input.files);
      this.archivos = [...this.archivos, ...nuevos];
      input.value = '';
    }
  }

  eliminarArchivo(index: number): void {
    this.archivos.splice(index, 1);
  }

  /**
   * Envía el formulario al backend después de validar los campos obligatorios.
   * Si es menor de edad, requiere la información del tutor.
   */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.esMenor && !this.tutorData) {
      this.mostrarModalTutor = true;
      return;
    }

    this.cargando = true;
    this.error = '';

    const v = this.form.value;
    const dto = {
      nombreQuejoso: v.nombreQuejoso!,
      primerApellido: v.primerApellido!,
      segundoApellido: v.segundoApellido ?? '',
      correo: v.correo!,
      fechaNacimiento: v.fechaNacimiento!,
      identificacionInstitucional: v.identificacionInstitucional!,
      tipoIdentificacion: v.tipoIdentificacion!,
      unidadAcademica: v.unidadAcademica!,
      fechaHechos: v.fechaHechos ? `${v.fechaHechos}T00:00:00` : '',
      asunto: "", // Se envía vacío por regla de negocio, lo llena recepción
      descripcionHechos: v.descripcionHechos!,
      nombreDenunciado: v.nombreDenunciado!,
      primerApellidoDenunciado: v.primerApellidoDenunciado!,
      tutor: this.tutorData ?? undefined,
    };

    this.quejaService.registrar(dto, this.archivos).subscribe({
      next: (queja) => {
        this.router.navigate(['/queja/exito'], {
          state: { folio: queja.folio, correo: v.correo },
        });
      },
      error: (err) => {
        this.error = err.error?.mensaje ?? 'Error al enviar la queja. Intenta de nuevo.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
