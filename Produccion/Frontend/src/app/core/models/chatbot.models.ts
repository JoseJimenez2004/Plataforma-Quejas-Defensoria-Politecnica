export interface PreguntaChatbot {
  id: number;
  pregunta: string;
  respuesta: string;
}

export interface CategoriaChatbot {
  categoria: string;
  preguntas: PreguntaChatbot[];
}

/** Un turno dentro de la conversación mostrada en el widget (no se persiste, solo vive en
 * memoria del componente mientras el panel está abierto). */
export interface MensajeChatbot {
  autor: 'bot' | 'usuario';
  texto: string;
}

/** Un botón de opción preseleccionada que el usuario puede pulsar en el turno actual. */
export interface OpcionChatbot {
  texto: string;
  accion: () => void;
}
