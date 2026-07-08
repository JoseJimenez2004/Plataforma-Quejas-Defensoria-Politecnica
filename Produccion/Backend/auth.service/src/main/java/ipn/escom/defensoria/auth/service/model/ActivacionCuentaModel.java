package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

@Data
public class ActivacionCuentaModel {
    private String correo;
    private String numeroFolio;
    private String password;
    private String confirmarPassword;
}