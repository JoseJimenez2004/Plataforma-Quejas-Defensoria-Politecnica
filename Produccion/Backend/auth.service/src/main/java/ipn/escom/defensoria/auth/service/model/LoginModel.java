package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

/** Body de POST /api/auth/login. */
@Data
public class LoginModel {
    private String correo;
    private String password;
}
