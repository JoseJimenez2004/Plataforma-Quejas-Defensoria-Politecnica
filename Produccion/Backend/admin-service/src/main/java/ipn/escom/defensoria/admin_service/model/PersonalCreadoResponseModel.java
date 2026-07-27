package ipn.escom.defensoria.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Respuesta al crear personal nuevo -- es la ÚNICA vez que la contraseña temporal viaja en
 * texto plano; después solo existe su hash en la base de datos. */
@Data
@AllArgsConstructor
public class PersonalCreadoResponseModel {
    private Long id;
    private String nombreCompleto;
    private String correoInstitucional;
    private String passwordTemporal;
}
