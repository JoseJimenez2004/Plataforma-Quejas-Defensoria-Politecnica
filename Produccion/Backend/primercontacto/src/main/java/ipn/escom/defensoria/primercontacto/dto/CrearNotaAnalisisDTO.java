package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearNotaAnalisisDTO {

    @NotBlank
    private String folio;

    @NotBlank
    private String contenido;
}