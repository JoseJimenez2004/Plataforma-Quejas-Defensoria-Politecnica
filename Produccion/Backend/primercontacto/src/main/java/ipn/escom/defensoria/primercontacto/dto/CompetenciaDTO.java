package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetenciaDTO {

    /*
     * Folio propio de Primer Contacto.
     * Ejemplo: PC-A1B2C3D4
     */
    @NotBlank
    private String folio;

    @NotBlank
    private String justificacion;

    @NotBlank
    private String areaTurno;

    @NotBlank
    private String responsableTurno;

    private String observaciones;
}