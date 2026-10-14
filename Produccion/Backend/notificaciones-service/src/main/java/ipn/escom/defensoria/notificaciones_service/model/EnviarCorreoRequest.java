package ipn.escom.defensoria.notificaciones_service.model;

import lombok.Data;

/** Body de POST /api/notificaciones/enviar -- correo saliente puro (no se persiste en el
 * centro de notificaciones). Lo usan revision-service (correo de rechazo con observaciones)
 * y queja-service (aviso al tutor cuando la queja es de un menor). Subdefensoría además adjunta
 * el PDF del oficio o del recordatorio (campos adjunto*, opcionales: sin ellos el correo sale
 * igual que siempre). */
@Data
public class EnviarCorreoRequest {
    private String destinatario;
    private String asunto;
    private String cuerpo;
    /** Nombre del archivo adjunto (opcional), p. ej. oficio-OF001-ESCOM-013.pdf. */
    private String adjuntoNombre;
    /** Tipo MIME del adjunto; si falta se usa application/pdf. */
    private String adjuntoTipo;
    /** Contenido del adjunto en base64. */
    private String adjuntoBase64;
}
