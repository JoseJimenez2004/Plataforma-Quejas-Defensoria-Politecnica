package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemisionDTO {

    private Long id;
    private Long quejaId;
    private String folio;
    private Long analistaId;
    private String analistaNombre;
    private String autoridadRemision;
    private String justificacionLegal;
    private String sugerenciaQuejoso;
    private Boolean adjuntarExpediente;
    private String fechaRemision;
}