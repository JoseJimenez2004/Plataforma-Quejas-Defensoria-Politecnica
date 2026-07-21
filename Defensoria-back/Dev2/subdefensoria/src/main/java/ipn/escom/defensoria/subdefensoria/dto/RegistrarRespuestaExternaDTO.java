package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarRespuestaExternaDTO {

    @NotNull
    private Long oficioId;

    @NotBlank
    private String canalRecepcion;

    private String numeroOficioRespuestaUA;

    private String archivoPdfPath;

    @NotBlank
    private String resumen;
}
