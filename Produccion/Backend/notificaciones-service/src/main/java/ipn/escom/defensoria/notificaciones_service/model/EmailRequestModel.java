package ipn.escom.defensoria.notificaciones_service.model;

import lombok.Data;

@Data
public class EmailRequestModel {
    private String destinatario;
    private String asunto;
    private String cuerpo;
}