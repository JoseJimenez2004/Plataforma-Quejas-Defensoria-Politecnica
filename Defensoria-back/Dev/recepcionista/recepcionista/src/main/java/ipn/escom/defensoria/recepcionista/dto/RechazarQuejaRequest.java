package ipn.escom.defensoria.recepcionista.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para rechazar una queja y notificar al quejoso.
 *
 * Se usa en dos endpoints:
 *   - PUT  /quejas/{id}/rechazar          → cambia estatus y guarda motivo en historial
 *   - POST /quejas/{id}/notificar-rechazo → registra que se notificó al quejoso
 *
 * @NotBlank valida que el campo no venga vacío ni nulo en el JSON.
 * Si viene vacío, el GlobalExceptionHandler devuelve 400 con el mensaje
 * "motivoRechazo: El motivo de rechazo es obligatorio".
 */
@Data
@NoArgsConstructor
public class RechazarQuejaRequest {

    @NotBlank(message = "El motivo de rechazo es obligatorio")
    private String motivoRechazo;
}
