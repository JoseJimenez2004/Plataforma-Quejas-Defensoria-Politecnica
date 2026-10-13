package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Respuesta del quejoso al acuerdo: el comentario es obligatorio solo para rechazar. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaAcuerdoDTO {

    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres.")
    private String comentario;
}
