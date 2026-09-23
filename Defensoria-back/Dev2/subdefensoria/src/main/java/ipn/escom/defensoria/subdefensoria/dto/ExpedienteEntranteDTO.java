package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Contrato de entrada del endpoint de ingesta
 * (POST /api/subdefensoria/ingesta/expedientes). Primer Contacto
 * llama aqui una sola vez por expediente, en el momento en que emite
 * el acuerdo de admision (act. 10 del DDP-PO-02) y lo turna a la
 * Subdefensoria (Abogado Asesor) que corresponda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEntranteDTO {

    @NotNull
    private Long quejaId;

    @NotBlank
    private String folio;

    @NotBlank
    private String asunto;

    private String descripcionHechos;

    @NotNull
    private LocalDate fechaAdmision;

    private Long abogadoAsesorId;

    private String abogadoAsesorNombre;

    @Valid
    @NotNull
    private QuejosoResumenDTO quejoso;

    private String observacionesAnalista;
}
