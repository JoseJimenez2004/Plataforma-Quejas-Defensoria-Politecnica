package ipn.escom.defensoria.revision_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistroManualResponse {
    private String numeroFolio;
    private String mensaje;
}
