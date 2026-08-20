package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Expediente recibido desde Primer Contacto.
 *
 * La relación entre áreas se realiza mediante el folio
 * de Primer Contacto, nunca mediante IDs internos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEntranteDTO {

    /*
     * Folio del expediente en Primer Contacto.
     *
     * Ejemplo:
     * PC-A1B2C3D4
     */
    @NotBlank
    private String folioOrigen;

    @NotBlank
    private String asunto;

    private String descripcionHechos;

    @NotNull
    private LocalDate fechaAdmision;

    private Long abogadoAsesorId;

    private String abogadoAsesorNombre;

    @Valid
    private QuejosoResumenDTO quejoso;

    private String observacionesAnalista;
}