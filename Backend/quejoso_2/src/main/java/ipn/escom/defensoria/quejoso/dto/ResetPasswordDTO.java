package ipn.escom.defensoria.quejoso.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String correo;
    private String codigo;
    private String nuevaPassword;
}