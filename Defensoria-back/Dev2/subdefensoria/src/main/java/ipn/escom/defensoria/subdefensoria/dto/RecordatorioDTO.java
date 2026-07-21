package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordatorioDTO {

    private Long id;
    private Long oficioId;
    private String numeroOficio;
    private String mensaje;
    private String medidasOfrecidas;
    private Integer diasRetraso;
    private String fechaEnvio;
    private String nuevaFechaLimite;
    private String estatusExpediente;
}
