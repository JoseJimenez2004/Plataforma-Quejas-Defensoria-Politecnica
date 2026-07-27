package ipn.escom.defensoria.catalogo_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportacionResumenModel {
    private int filasCreadas;
    private int filasActualizadas;
    private List<String> errores;
}
