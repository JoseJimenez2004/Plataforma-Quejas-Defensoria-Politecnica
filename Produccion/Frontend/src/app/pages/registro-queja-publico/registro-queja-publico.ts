import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface DatosTutor {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  parentesco: string;
  correo: string;
  telefono: string;
}

@Component({
  selector: 'app-registro-queja-publico',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './registro-queja-publico.html',
  styleUrl: './registro-queja-publico.scss',
})
export class RegistroQuejaPublico {
  // Datos del quejoso
  nombre = '';
  apellidoPaterno = '';
  apellidoMaterno = '';
  correo = '';
  fechaNacimiento = '';
  identificacion: 'alumno' | 'empleado' = 'alumno';

  // Datos de la queja
  unidadAcademica = '';
  fechaHechos = '';

  // Datos del denunciado
  nombreDenunciado = '';
  apellidoDenunciado = '';
  segundoApellidoDenunciado = '';
  descripcion = '';
  archivo: File | null = null;

  // Menor de edad / tutor
  mostrarModalTutor = false;
  tutor: DatosTutor = {
    nombre: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    parentesco: 'Padre',
    correo: '',
    telefono: '',
  };

  enviado = false;
  mensajeBackendPendiente = false;

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo = input.files?.[0] ?? null;
  }

  onFechaNacimientoChange(): void {
    if (!this.fechaNacimiento) return;
    const edad = this.calcularEdad(this.fechaNacimiento);
    if (edad < 18) {
      this.mostrarModalTutor = true;
    }
  }

  private calcularEdad(fechaISO: string): number {
    const nacimiento = new Date(fechaISO);
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();
    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  }

  confirmarTutor(): void {
    this.mostrarModalTutor = false;
  }

  cancelarTutor(): void {
    this.fechaNacimiento = '';
    this.mostrarModalTutor = false;
  }

  enviarQueja(): void {
    // El backend actual (queja-service) solo expone POST /api/quejoso/quejas/registrar
    // protegido por JWT, derivando el correo del token de sesión — no acepta un registro
    // público/anónimo con los datos del quejoso como en este formulario. Hace falta un
    // endpoint público nuevo en el backend para que este formulario funcione de punta a
    // punta. Ver docs/HALLAZGOS.md. Por ahora, mostramos esto en vez de fingir que funciona.
    this.mensajeBackendPendiente = true;
  }
}
