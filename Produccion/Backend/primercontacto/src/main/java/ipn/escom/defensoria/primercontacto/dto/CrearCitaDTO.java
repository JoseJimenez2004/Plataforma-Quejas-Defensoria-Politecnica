package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearCitaDTO {

    @NotNull
    private Long quejaId;

    @NotBlank
    private String folio;

    private Long quejosoId;

    private String quejosoNombre;

    @NotNull
    private Long analistaId;

    @NotBlank
    private String analistaNombre;

    @NotBlank
    private String fechaCita;

    @NotBlank
    private String horaCita;

    @NotBlank
    private String tipoCita;

    @NotBlank
    private String motivo;
}