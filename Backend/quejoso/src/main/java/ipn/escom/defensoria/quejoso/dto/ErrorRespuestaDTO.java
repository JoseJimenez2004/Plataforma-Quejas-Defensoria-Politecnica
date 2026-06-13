package ipn.escom.defensoria.quejoso.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorRespuestaDTO {
    private String mensaje;
    private LocalDateTime timestamp;
    private int codigo;
}