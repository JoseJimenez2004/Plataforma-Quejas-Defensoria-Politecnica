package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

/** Body de POST /api/auth/activar-cuenta: activación "Just-in-Time" a partir de un folio +
 * correo ya validados contra queja-service, definiendo la contraseña de la cuenta nueva. */
@Data
public class ActivacionCuentaModel {
    private String numeroFolio;
    private String correo;
    private String password;
    private String confirmarPassword;
}
