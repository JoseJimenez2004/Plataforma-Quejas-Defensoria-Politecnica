package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

@Data
public class ResetPasswordModel {
    private String correo;
    private String codigo;
    private String nuevaPassword;
}