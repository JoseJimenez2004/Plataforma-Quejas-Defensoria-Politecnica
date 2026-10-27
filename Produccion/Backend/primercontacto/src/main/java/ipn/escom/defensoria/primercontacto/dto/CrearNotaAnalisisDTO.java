package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 200)
    private String asunto;

    @NotBlank
    private String contenido;
}