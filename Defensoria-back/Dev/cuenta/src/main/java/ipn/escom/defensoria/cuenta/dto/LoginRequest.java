package ipn.escom.defensoria.cuenta.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para capturar las credenciales de la Pantalla de Login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String correo;
    private String password;

}