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

    @NotNull
    private Long quejaId;

    @NotBlank
    private String folio;

    @NotNull
    private Long analistaId;

    @NotBlank
    private String analistaNombre;

    @NotBlank
    private String justificacion;

    @NotBlank
    private String areaTurno;

    @NotBlank
    private String responsableTurno;
}