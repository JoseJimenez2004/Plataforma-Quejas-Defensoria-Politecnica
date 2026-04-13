package ipn.escom.defensoria.recepcionista.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para la acción "Confirmar Canalización".
 *
 * Se usa en: POST /quejas/{id}/canalizar
 *
 * Campos:
 * - opcionCanalizacion → obligatorio. La opción elegida en el dropdown
 *   de la pantalla (ej. "Turno a abogado", "Orientación", "Derivar a otra instancia")
 * - notasAdicionales   → opcional. Comentarios libres de la recepcionista
 *   sobre la canalización (ej. "Caso urgente, hay evidencia fotográfica")
 *
 * Ambos campos se guardan como comentario en historial_estatus_queja.
 */
@Data
@NoArgsConstructor
public class CanarizarRequest {

    @NotBlank(message = "La opción de canalización es obligatoria")
    private String opcionCanalizacion;

    private String notasAdicionales;
}
