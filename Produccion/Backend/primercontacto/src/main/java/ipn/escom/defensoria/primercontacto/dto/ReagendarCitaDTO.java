package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/*
 * Nueva fecha/hora de una cita existente (CU-PC-04).
 * Tipo y motivo son opcionales: si no vienen, se conservan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReagendarCitaDTO {

    @NotBlank
    private String fechaCita;

    @NotBlank
    private String horaCita;

    private String tipoCita;

    private String motivo;
}
