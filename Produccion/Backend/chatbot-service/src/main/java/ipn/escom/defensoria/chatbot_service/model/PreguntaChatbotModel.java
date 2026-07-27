package ipn.escom.defensoria.chatbot_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versión pública (sin categoria/orden/activo repetidos) de una pregunta, ya agrupada
 * dentro de {@link CategoriaChatbotModel}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaChatbotModel {
    private Long id;
    private String pregunta;
    private String respuesta;
}
