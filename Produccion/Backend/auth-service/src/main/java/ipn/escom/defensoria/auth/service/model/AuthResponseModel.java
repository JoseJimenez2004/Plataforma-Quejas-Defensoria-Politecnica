package ipn.escom.defensoria.auth.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Respuesta de POST /api/auth/login: el JWT emitido y el nombre para mostrar en el panel. */
@Data
@AllArgsConstructor
public class AuthResponseModel {
    private String token;
    private String nombre;
}
