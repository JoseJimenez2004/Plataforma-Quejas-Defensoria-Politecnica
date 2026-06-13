package ipn.escom.defensoria.quejoso.dto;

import lombok.Data;

@Data
public class ActivacionCuentaDTO {
    private String correo;
    private String numeroFolio;
    private String password;
    private String confirmarPassword;
}