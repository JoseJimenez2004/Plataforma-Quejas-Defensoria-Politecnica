import { Component } from '@angular/core';

import { ChatbotService } from '../../core/services/chatbot.service';
import { CategoriaChatbot, MensajeChatbot, OpcionChatbot, PreguntaChatbot } from '../../core/models/chatbot.models';

/**
 * Icono tipo mascota flotante que abre un mini-chat de preguntas preseleccionadas sobre la
 * Defensoría y el proceso para presentar una queja (tutorial). Deliberadamente NO usa IA/LLM:
 * todo el contenido (categorías, preguntas y respuestas) viene ya redactado desde
 * chatbot-service, con base en el Acuerdo de creación de la DDP y su Manual de
 * Procedimientos, para no correr el riesgo de que se invente información institucional
 * incorrecta. Ver Backend/chatbot-service.
 */
@Component({
  selector: 'app-chatbot-widget',
  standalone: true,
  imports: [],
  templateUrl: './chatbot-widget.html',
  styleUrl: './chatbot-widget.scss',
})
export class ChatbotWidget {
  abierto = false;
  cargando = false;
  error = false;

  categorias: CategoriaChatbot[] = [];
  mensajes: MensajeChatbot[] = [];
  opciones: OpcionChatbot[] = [];

  private categoriaActual: CategoriaChatbot | null = null;
  private yaSaludo = false;

  constructor(private chatbotService: ChatbotService) {}

  alternarPanel(): void {
    this.abierto = !this.abierto;
    if (this.abierto && this.categorias.length === 0 && !this.cargando && !this.error) {
      this.cargarMenu();
    }
  }

  cerrar(): void {
    this.abierto = false;
  }

  private cargarMenu(): void {
    this.cargando = true;
    this.chatbotService.obtenerMenu().subscribe({
      next: (categorias) => {
        this.categorias = categorias;
        this.cargando = false;
        this.mostrarMenuPrincipal();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.mensajes = [
          {
            autor: 'bot',
            texto:
              'No pude cargar la información en este momento. Puedes escribirnos directamente a ' +
              'quejasddp@ipn.mx o llamar al 55 5729 6000, ext. 57277 y 57278.',
          },
        ];
        this.opciones = [];
      },
    });
  }

  private mostrarMenuPrincipal(): void {
    this.categoriaActual = null;
    if (!this.yaSaludo) {
      this.mensajes.push({
        autor: 'bot',
        texto:
          '¡Hola! Soy el asistente de la Defensoría de los Derechos Politécnicos. Elige un tema ' +
          'para conocer más o para que te guíe paso a paso al presentar tu queja.',
      });
      this.yaSaludo = true;
    } else {
      this.mensajes.push({ autor: 'bot', texto: '¿Sobre qué más te gustaría saber?' });
    }
    this.opciones = this.categorias.map((categoria) => ({
      texto: categoria.categoria,
      accion: () => this.seleccionarCategoria(categoria),
    }));
  }

  private seleccionarCategoria(categoria: CategoriaChatbot): void {
    this.categoriaActual = categoria;
    this.mensajes.push({ autor: 'usuario', texto: categoria.categoria });
    this.mensajes.push({
      autor: 'bot',
      texto: `Estas son las preguntas sobre "${categoria.categoria}":`,
    });
    this.opciones = [
      ...categoria.preguntas.map((pregunta) => ({
        texto: pregunta.pregunta,
        accion: () => this.seleccionarPregunta(categoria, pregunta),
      })),
      { texto: '⬅ Ver otras categorías', accion: () => this.mostrarMenuPrincipal() },
    ];
  }

  private seleccionarPregunta(categoria: CategoriaChatbot, pregunta: PreguntaChatbot): void {
    this.mensajes.push({ autor: 'usuario', texto: pregunta.pregunta });
    this.mensajes.push({ autor: 'bot', texto: pregunta.respuesta });

    const otras = categoria.preguntas.filter((p) => p.id !== pregunta.id);
    this.opciones = [
      ...otras.map((p) => ({
        texto: p.pregunta,
        accion: () => this.seleccionarPregunta(categoria, p),
      })),
      { texto: '⬅ Ver otras categorías', accion: () => this.mostrarMenuPrincipal() },
    ];
  }

  elegir(opcion: OpcionChatbot): void {
    opcion.accion();
  }
}
