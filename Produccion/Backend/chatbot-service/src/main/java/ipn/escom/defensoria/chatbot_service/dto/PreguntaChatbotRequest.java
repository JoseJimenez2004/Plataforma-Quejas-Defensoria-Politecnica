package ipn.escom.defensoria.chatbot_service.dto;

import lombok.Data;

@Data
public class PreguntaChatbotRequest {
    private String categoria;
    private String pregunta;
    private String respuesta;
    private Integer orden;
    private Boolean activo;
}
