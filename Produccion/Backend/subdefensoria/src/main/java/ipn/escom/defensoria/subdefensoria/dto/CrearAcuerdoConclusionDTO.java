package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** TS-07: redactar el acuerdo final. concluir=true cierra el expediente (TS-08); false solo guarda borrador. */
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
