package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerarRecordatorioDTO {

    @NotNull
    private Long oficioId;

    @NotBlank
    private String mensaje;

    /** Solo aplica en TS-06 (fase GESTION_DIRECTOR); texto libre por ahora. */
    private String medidasOfrecidas;
}
