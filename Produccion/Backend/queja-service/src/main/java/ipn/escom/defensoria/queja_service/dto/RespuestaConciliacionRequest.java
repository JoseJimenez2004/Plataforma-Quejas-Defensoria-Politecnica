package ipn.escom.defensoria.queja_service.dto;

import lombok.Data;

/** Body de PUT /api/quejoso/conciliaciones/{id}/respuesta. */
@Data
public class RespuestaConciliacionRequest {
    /** "ACEPTADO" | "RECHAZADO" */
    private String estado;
    private String comentario;
}
