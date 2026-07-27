package ipn.escom.defensoria.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RespaldoResumenModel {
    private String nombreArchivo;
    private long tamanioBytes;
    private String fecha;
}
