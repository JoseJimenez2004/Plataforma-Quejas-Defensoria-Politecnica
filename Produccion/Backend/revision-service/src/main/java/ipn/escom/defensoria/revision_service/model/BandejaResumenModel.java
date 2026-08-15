package ipn.escom.defensoria.revision_service.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BandejaResumenModel {
    private long pendientes;
    private long enProceso;
    private long turnadasHoy;
    private List<QuejaResumenBandejaModel> lista;
}
