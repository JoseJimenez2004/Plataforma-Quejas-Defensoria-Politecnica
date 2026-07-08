package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

@Data
public class LoginModel {
    private String correo;
    private String password;
}