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

    @NotNull
    private Long quejaId;

    @NotBlank
    private String folio;

    @NotNull
    private Long analistaId;

    @NotBlank
    private String analistaNombre;

    @NotBlank
    private String autoridadRemision;

    @NotBlank
    private String justificacionLegal;

    private String sugerenciaQuejoso;

    @NotNull
    private Boolean adjuntarExpediente;
}