package ipn.escom.defensoria.chatbot_service.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una categoría del menú del mini-chat (ej. "Sobre la Defensoría") con sus preguntas ya
 * en el orden en que deben mostrarse los botones. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaChatbotModel {
    private String categoria;
    private List<PreguntaChatbotModel> preguntas;
}
