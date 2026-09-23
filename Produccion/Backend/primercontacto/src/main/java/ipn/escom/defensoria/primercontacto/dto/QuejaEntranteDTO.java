package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Contrato de entrada: esto es lo que el área de Subdefensoría envía
 * (vía POST) cuando existe una queja nueva o actualizada que debe
 * pasar por Primer Contacto.
 * No se persiste en BD: se guarda en memoria mientras el servicio
 * está arriba, ya que la fuente de verdad de la queja vive en
 * Subdefensoría.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejaEntranteDTO {

    @NotNull
    private Long quejaId;

    @NotBlank
    private String folio;

    private String tema;

    private String descripcionHechos;

    private String fechaRecepcion;

    private String prioridad;

    @Valid
    private QuejosoDTO quejoso;

    private List<EvidenciaDTO> evidencias;
}
