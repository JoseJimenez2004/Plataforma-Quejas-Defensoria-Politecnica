package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** CU-SD-09: acuerdo de conciliación. concluir=true lo envía al quejoso; false solo guarda borrador. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAcuerdoConclusionDTO {

    @NotNull
    private Long expedienteId;

    @NotBlank
    private String textoAcuerdo;

    @NotNull
    private Boolean concluir;
}
