package ipn.escom.defensoria.notificaciones_service.model;

import lombok.Data;

/** Body de POST /api/notificaciones/enviar -- correo saliente puro (no se persiste en el
 * centro de notificaciones). Lo usan revision-service (correo de rechazo con observaciones)
 * y queja-service (aviso al tutor cuando la queja es de un menor). */
@Data
public class EnviarCorreoRequest {
    private String destinatario;
    private String asunto;
    private String cuerpo;
}
