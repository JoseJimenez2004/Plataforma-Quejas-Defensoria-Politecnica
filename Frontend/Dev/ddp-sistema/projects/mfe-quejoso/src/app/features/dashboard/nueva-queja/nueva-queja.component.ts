import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidatorFn } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { QuejaService } from '../../../core/services/queja.service';
import { AuthService } from '../../../core/services/auth.service';

const UNIDADES = [
  // Nivel Superior
  'ESCOM',
  'ESFM',
  'ESIA Unidad Tecamachalco',
  'ESIA Unidad Ticomán',
  'ESIA Unidad Zacatenco',
  'ESIME Unidad Azcapotzalco',
  'ESIME Unidad Culhuacán',
  'ESIME Unidad Ticomán',
  'ESIME Unidad Zacatenco',
  'ESIQIE',
  'ESIT',
  'CICS Unidad Milpa Alta',
  'CICS Unidad Santo Tomás',
  'ENCB',
  'ENMH',
  'ESEO',
  'ESM',
  'ESE',
  'ESCA Unidad Santo Tomás',
  'ESCA Unidad Tepepan',
  'EST',
  'ENBA',
  'UPIBI',
  'UPIIG Campus Guanajuato',
  'UPIIH Campus Hidalgo',
  'UPIIZ Campus Zacatecas',
  'UPIEM',
  'UPIICSA',
  'UPIITA',
  'UPIIP Campus Palenque',
  'UPIIT Campus Tlaxcala',
  // Nivel Medio Superior
  'CECyT 1 "Gonzalo Vázquez Vela"',
  'CECyT 2 "Miguel Bernard Perales"',
  'CECyT 3 "Estanislao Ramírez Ruíz"',
  'CECyT 4 "Lázaro Cárdenas"',
  'CECyT 5 "Benito Juárez García"',
  'CECyT 6 "Miguel Othón de Mendizábal"',
  'CECyT 7 "Cuauhtémoc"',
  'CECyT 8 "Narciso Bassols García"',
  'CECyT 9 "Juan de Dios Bátiz Paredes"',
  'CECyT 10 "Carlos Vallejo Márquez"',
  'CECyT 11 "Wilfrido Massieu"',
  'CECyT 12 "José María Morelos"',
  'CECyT 13 "Ricardo Flores Magón"',
  'CECyT 14 "Luis Enrique Erro Soler"',
  'CECyT 15 "Diódoro Antúnez Echagaray"',
  'CECyT 16 "Unidad Hidalgo"',
  'CECyT 17 "Unidad Guanajuato"',
  'CECyT 18 "Unidad Zacatecas"',
  'CECyT 19 "Leona Vicario" Unidad Tecámac',
  'CET 1 "Walter Cross Buchanan"',
  'OTRA',
];

@Component({
  selector: 'app-nueva-queja',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './nueva-queja.component.html',
})
export class NuevaQuejaComponent implements OnInit {
  private fb = inject(FormBuilder);
  private quejaService = inject(QuejaService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  unidades = UNIDADES;
  archivos: File[] = [];
  cargando = false;
  error = '';

  form = this.fb.group({
    unidadAcademica: ['', Validators.required],
    fechaHechos: ['', [Validators.required, this.fechaValidaValidator()]],
    nombreDenunciado: ['', Validators.required],
    primerApellidoDenunciado: ['', Validators.required],
    segundoApellidoDenunciado: [''],
    descripcionHechos: ['', [Validators.required, Validators.minLength(30)]],
  });

  get hoy(): string {
    return new Date().toISOString().split('T')[0];
  }

  fechaValidaValidator(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      if (!control.value) return null;
      
      const fechaIngresada = new Date(control.value);
      // Para evitar problemas con la zona horaria al comparar fechas
      fechaIngresada.setHours(0, 0, 0, 0);
      
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      
      const noventaDiasAtras = new Date();
      noventaDiasAtras.setDate(hoy.getDate() - 90);
      noventaDiasAtras.setHours(0, 0, 0, 0);

      if (fechaIngresada > hoy) {
        return { futura: true };
      }
      
      if (fechaIngresada < noventaDiasAtras) {
        return { antigua: true };
      }
      
      return null;
    };
  }

  ngOnInit(): void {}

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.archivos = [...this.archivos, ...Array.from(input.files)];
      input.value = '';
    }
  }

  eliminarArchivo(i: number): void {
    this.archivos.splice(i, 1);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const perfil = this.authService.getPerfil();
    if (!perfil) return;

    this.cargando = true;
    this.error = '';

    const v = this.form.value;
    const dto = {
      nombreQuejoso: perfil.nombre,
      primerApellido: '',
      segundoApellido: '',
      correo: perfil.correoInstitucional,
      fechaNacimiento: '',
      identificacionInstitucional: perfil.boleta,
      tipoIdentificacion: 'ALUMNO',
      unidadAcademica: v.unidadAcademica!,
      fechaHechos: v.fechaHechos!,
      asunto: "", // Se envía vacío por regla de negocio
      descripcionHechos: v.descripcionHechos!,
      nombreDenunciado: v.nombreDenunciado!,
      primerApellidoDenunciado: v.primerApellidoDenunciado!,
      segundoApellidoDenunciado: v.segundoApellidoDenunciado ?? '',
    };

    this.quejaService.registrar(dto, this.archivos).subscribe({
      next: (queja) => {
        this.router.navigate(['/dashboard'], {
          state: { folioNuevo: queja.folio },
        });
      },
      error: (err) => {
        this.error = err.error?.mensaje ?? 'Error al enviar la queja.';
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
