package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/*
 * Propuesta de acuerdo de conciliación desde Primer Contacto (CU-PC-10).
 * El correo del quejoso se resuelve a partir de la queja, no se recibe.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearConciliacionDTO {

    /* Folio propio de Primer Contacto: PC-XXXXXXXX */
    @NotBlank
    private String folio;

    @NotBlank
    @Size(max = 255)
    private String asunto;

    @NotBlank
    private String terminos;
}
