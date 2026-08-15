package ipn.escom.defensoria.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Se regresa una sola vez al admin que ejecuta el reset -- la contraseña temporal en texto
 * plano nunca se vuelve a poder consultar después (solo queda su hash). */
@Data
@AllArgsConstructor
public class ResetPasswordResponseModel {
    private String passwordTemporalNueva;
}
