package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearRemisionDTO {

    @NotBlank
    private String folio;

    @NotBlank
    private String autoridadRemision;

    @NotBlank
    private String justificacionLegal;

    private String sugerenciaQuejoso;

    @NotNull
    private Boolean adjuntarExpediente;
}