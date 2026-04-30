package ipn.escom.defensoria.login.dto;

import lombok.Data;

@Data
public class RegistroRequest {
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private String password;
    private String nombreRol;
}