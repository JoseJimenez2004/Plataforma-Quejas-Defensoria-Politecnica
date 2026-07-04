package ipn.escom.defensoria.quejoso.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerificacionCuentaDTO {
    private boolean existeCuenta;
    private boolean cuentaActiva;
}
