package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaVencimientoDTO {

    private Long oficioId;
    private String numeroOficio;
    private String folio;
    private String unidadAcademica;
    private String fase;
    private String fechaLimite;
    private long diasRetraso;
}
