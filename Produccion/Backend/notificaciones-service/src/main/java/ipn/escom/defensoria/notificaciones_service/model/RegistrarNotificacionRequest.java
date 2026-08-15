package ipn.escom.defensoria.notificaciones_service.model;

import lombok.Data;

/** Body de POST /api/notificaciones/registrar -- llamada interna desde otros microservicios
 * (auth-service en login, queja-service al crear una queja, revision-service en cambios de
 * estatus) para dejar un aviso persistido en el centro de notificaciones del quejoso. A
 * diferencia de /enviar (correo saliente puro), esto NO manda correo, solo persiste. */
@Data
public class RegistrarNotificacionRequest {
    private String correoDestino;
    /** LOGIN | QUEJA_CREADA | CAMBIO_ESTATUS | CONCILIACION | GENERAL */
    private String tipo;
    private String titulo;
    private String mensaje;
    private String enlace;
}
