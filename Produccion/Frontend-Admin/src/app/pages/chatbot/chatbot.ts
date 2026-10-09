import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ChatbotAdminService } from '../../core/services/chatbot-admin.service';
import { ToastService } from '../../core/services/toast.service';
import { PreguntaChatbot, PreguntaChatbotRequest } from '../../core/models/admin.models';

const PREGUNTA_MAXIMA = 300;
const CATEGORIA_MAXIMA = 80;

/** CU-BOT-01: alta, edición y baja de las preguntas del chatbot del portal del quejoso. */
@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.scss',
})
export class Chatbot implements OnInit {
  readonly preguntaMaxima = PREGUNTA_MAXIMA;
  readonly categoriaMaxima = CATEGORIA_MAXIMA;

  preguntas: PreguntaChatbot[] = [];
  cargando = true;

  filtroTexto = '';
  filtroCategoria = '';
  filtroEstado: '' | 'activas' | 'inactivas' = '';

  modalAbierto = false;
  editando: PreguntaChatbot | null = null;
  formulario: PreguntaChatbotRequest = this.vacio();
  errorFormulario = '';
  guardando = false;

  porEliminar: PreguntaChatbot | null = null;

  constructor(
    private chatbotService: ChatbotAdminService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  get categorias(): string[] {
    return [...new Set(this.preguntas.map((p) => p.categoria))].sort((a, b) => a.localeCompare(b, 'es'));
  }

  get filtradas(): PreguntaChatbot[] {
    const texto = this.normalizar(this.filtroTexto);
    return this.preguntas
      .filter((p) => !this.filtroCategoria || p.categoria === this.filtroCategoria)
      .filter((p) => this.filtroEstado === '' || (this.filtroEstado === 'activas') === p.activo)
      .filter(
        (p) =>
          !texto ||
          this.normalizar(p.pregunta).includes(texto) ||
          this.normalizar(p.respuesta).includes(texto) ||
          this.normalizar(p.categoria).includes(texto),
      )
      .sort((a, b) => a.categoria.localeCompare(b.categoria, 'es') || a.orden - b.orden);
  }

  get totalActivas(): number {
    return this.preguntas.filter((p) => p.activo).length;
  }

  // ---------------- modal ----------------

  nueva(): void {
    this.editando = null;
    const siguienteOrden = this.preguntas.reduce((max, p) => Math.max(max, p.orden ?? 0), 0) + 1;
    this.formulario = { ...this.vacio(), categoria: this.filtroCategoria, orden: siguienteOrden };
    this.errorFormulario = '';
    this.modalAbierto = true;
  }

  editar(p: PreguntaChatbot): void {
    this.editando = p;
    this.formulario = {
      categoria: p.categoria,
      pregunta: p.pregunta,
      respuesta: p.respuesta,
      orden: p.orden,
      activo: p.activo,
    };
    this.errorFormulario = '';
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.guardando = false;
  }

  guardar(): void {
    const datos: PreguntaChatbotRequest = {
      categoria: this.formulario.categoria.trim(),
      pregunta: this.formulario.pregunta.trim(),
      respuesta: this.formulario.respuesta.trim(),
      orden: Number(this.formulario.orden) || 0,
      activo: this.formulario.activo,
    };
    if (!datos.categoria || !datos.pregunta || !datos.respuesta) {
      this.errorFormulario = 'Completa categoría, pregunta y respuesta.';
      return;
    }
    if (datos.pregunta.length > PREGUNTA_MAXIMA || datos.categoria.length > CATEGORIA_MAXIMA) {
      this.errorFormulario = `La pregunta admite ${PREGUNTA_MAXIMA} caracteres y la categoría ${CATEGORIA_MAXIMA}.`;
      return;
    }

    this.guardando = true;
    const peticion = this.editando
      ? this.chatbotService.editar(this.editando.id, datos)
      : this.chatbotService.crear(datos);
    peticion.subscribe({
      next: () => {
        this.toast.exito(this.editando ? 'Pregunta actualizada.' : 'Pregunta creada.');
        this.cerrarModal();
        this.cargar();
      },
      error: (err) => {
        this.guardando = false;
        this.errorFormulario = err?.error?.mensaje ?? 'No se pudo guardar la pregunta.';
        this.cdr.detectChanges();
      },
    });
  }

  // ---------------- activar / eliminar ----------------

  alternarActivo(p: PreguntaChatbot): void {
    const datos: PreguntaChatbotRequest = {
      categoria: p.categoria,
      pregunta: p.pregunta,
      respuesta: p.respuesta,
      orden: p.orden,
      activo: !p.activo,
    };
    this.chatbotService.editar(p.id, datos).subscribe({
      next: () => {
        p.activo = !p.activo;
        this.toast.exito(p.activo ? 'La pregunta ya se muestra en el chatbot.' : 'La pregunta se ocultó del chatbot.');
        this.cdr.detectChanges();
      },
      error: (err) => this.toast.error(err?.error?.mensaje ?? 'No se pudo cambiar el estado.'),
    });
  }

  confirmarEliminar(p: PreguntaChatbot): void {
    this.porEliminar = p;
  }

  eliminar(): void {
    const p = this.porEliminar;
    if (!p) {
      return;
    }
    this.chatbotService.eliminar(p.id).subscribe({
      next: () => {
        this.toast.exito('Pregunta eliminada.');
        this.porEliminar = null;
        this.cargar();
      },
      error: (err) => {
        this.porEliminar = null;
        this.toast.error(err?.error?.mensaje ?? 'No se pudo eliminar la pregunta.');
        this.cdr.detectChanges();
      },
    });
  }

  // ---------------- utilidades ----------------

  private cargar(): void {
    this.cargando = true;
    this.chatbotService.listar().subscribe({
      next: (lista) => {
        this.preguntas = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('No se pudieron cargar las preguntas del chatbot.');
        this.cdr.detectChanges();
      },
    });
  }

  private vacio(): PreguntaChatbotRequest {
    return { categoria: '', pregunta: '', respuesta: '', orden: 0, activo: true };
  }

  private normalizar(texto: string | null | undefined): string {
    return (texto ?? '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }
}
