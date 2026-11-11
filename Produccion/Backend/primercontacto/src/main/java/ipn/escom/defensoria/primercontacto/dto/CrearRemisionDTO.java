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

    /* Clave de la instancia (OAG, OIC, COSECOVI); el nombre y el correo salen del catálogo. */
    @NotBlank
    private String instanciaClave;

    @NotBlank
    private String justificacionLegal;

    private String sugerenciaQuejoso;

    @NotNull
    private Boolean adjuntarExpediente;
}