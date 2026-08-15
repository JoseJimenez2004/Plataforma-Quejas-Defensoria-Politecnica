package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuerdoConclusionDTO {

    private Long id;
    private Long expedienteId;
    private String folio;
    private String textoAcuerdo;
    private Boolean concluido;
    private String fechaCreacion;
    private String fechaEnvioSecretarial;
    private String estatusExpediente;
}
