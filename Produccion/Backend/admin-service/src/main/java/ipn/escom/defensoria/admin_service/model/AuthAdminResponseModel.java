package ipn.escom.defensoria.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthAdminResponseModel {
    private String token;
    private String nombre;
    private String rol;
    private boolean forzarCambioPassword;
}
