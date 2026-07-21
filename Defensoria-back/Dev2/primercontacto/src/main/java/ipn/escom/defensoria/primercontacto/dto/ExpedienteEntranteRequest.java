package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEntranteRequest {
    private Long quejaId;
    private String folio;
    private String asunto;
    private String descripcionHechos;
    private LocalDate fechaAdmision;
    private Long abogadoAsesorId;
    private String abogadoAsesorNombre;
    private QuejosoResumenRequest quejoso;
    private String observacionesAnalista;
}