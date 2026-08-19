package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteInvestigacionDTO {

    private Long id;
    private String folio;
    private String folioOrigen;
    private String quejosoNombre;
    private String unidadAcademica;
    private String asunto;
    private String descripcionHechos;
    private String fechaAdmision;
    private Long abogadoAsesorId;
    private String abogadoAsesorNombre;
    private String estatus;
    private String observacionesAnalista;
}
