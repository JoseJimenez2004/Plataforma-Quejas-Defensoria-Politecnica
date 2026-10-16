package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** CU-SD-09: texto del acuerdo de conclusión, una vez aceptado el de conciliación. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarConclusionDTO {

    @NotBlank(message = "Escribe el texto del acuerdo de conclusión.")
    private String textoConclusion;
}
