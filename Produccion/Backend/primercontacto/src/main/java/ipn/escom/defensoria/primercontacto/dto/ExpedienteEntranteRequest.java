package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEntranteRequest {

    /*
     * Folio propio de Primer Contacto.
     * Este será el folioOrigen en Subdefensoría.
     */
    private String folioOrigen;

    private String asunto;

    private String descripcionHechos;

    private LocalDate fechaAdmision;

    private Long abogadoAsesorId;

    private String abogadoAsesorNombre;

    private QuejosoResumenRequest quejoso;

    private String observacionesAnalista;
}