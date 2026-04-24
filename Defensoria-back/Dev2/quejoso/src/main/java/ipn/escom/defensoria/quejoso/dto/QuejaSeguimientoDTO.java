package ipn.escom.defensoria.quejoso.dto;

import ipn.escom.defensoria.quejoso.entity.EstatusQuejaEntity;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuejaSeguimientoDTO {
    private String folio;
    private LocalDateTime fechaRegistro;
    private String asunto;
    private String descripcionHechos;
    private EstatusQuejaEntity estatusActual;
    private List<EvidenciaDTO> evidencias;
}