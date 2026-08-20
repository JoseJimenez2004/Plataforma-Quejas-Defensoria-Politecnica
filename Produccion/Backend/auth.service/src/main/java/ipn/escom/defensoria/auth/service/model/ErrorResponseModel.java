package ipn.escom.defensoria.auth.service.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Cuerpo JSON uniforme que regresa GlobalExceptionHandler para cualquier error (400 o 500),
 * en vez de dejar que Spring devuelva su página de error por defecto. */
@Data
@AllArgsConstructor
public class ErrorResponseModel {
    private String mensaje;
    private LocalDateTime timestamp;
    private int status;
}
