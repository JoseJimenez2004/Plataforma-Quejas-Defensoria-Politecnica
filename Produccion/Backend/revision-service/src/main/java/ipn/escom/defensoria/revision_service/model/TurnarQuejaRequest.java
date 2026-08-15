package ipn.escom.defensoria.revision_service.model;

import lombok.Data;

@Data
public class TurnarQuejaRequest {
    private String areaTurnada;
    private String defensorAsignado;
    private String comentarios;
}
