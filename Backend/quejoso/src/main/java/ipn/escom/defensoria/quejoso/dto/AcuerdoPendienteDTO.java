package ipn.escom.defensoria.quejoso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcuerdoPendienteDTO {
    private String folio;
    private LocalDateTime fechaRegistro;
    private String asunto;
    private String unidadAcademica;
}
