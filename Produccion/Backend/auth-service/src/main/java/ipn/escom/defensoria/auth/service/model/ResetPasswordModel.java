package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

/** Body de POST /api/auth/reset-password: código de verificación recibido por correo +
 * la nueva contraseña que el usuario quiere establecer. */
@Data
public class ResetPasswordModel {
    private String correo;
    private String codigo;
    private String nuevaPassword;
}
