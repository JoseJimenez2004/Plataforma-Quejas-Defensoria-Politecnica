package ipn.escom.defensoria.quejoso.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TramitesResumenDTO {
    private long totales;
    private long enProceso; // Suma de Recibida, Análisis, Investigación, etc.
    private long finalizadas;
}