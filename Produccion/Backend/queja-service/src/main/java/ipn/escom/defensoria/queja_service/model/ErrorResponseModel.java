package ipn.escom.defensoria.queja_service.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseModel {
    private String mensaje;
    private LocalDateTime timestamp;
    private int codigo;
}
