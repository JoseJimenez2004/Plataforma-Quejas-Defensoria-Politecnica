package ipn.escom.defensoria.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardResumenModel {
    private long totalPersonalActivo;
    private long totalDependencias;
    private String ultimoRespaldo;
    private long totalPlantillasActivas;
}
