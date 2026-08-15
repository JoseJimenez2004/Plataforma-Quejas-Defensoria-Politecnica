package ipn.escom.defensoria.revision_service.model;

import java.util.List;

import lombok.Data;

@Data
public class RechazarQuejaRequest {
    private List<String> motivos;
    private String observaciones;
}
