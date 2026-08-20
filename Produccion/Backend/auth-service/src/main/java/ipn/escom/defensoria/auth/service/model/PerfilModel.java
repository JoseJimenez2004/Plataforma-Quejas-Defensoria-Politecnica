package ipn.escom.defensoria.auth.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de GET /api/auth/me -- antes el frontend solo tenía nombre+correo (lo que
 * regresaba /login), sin boleta/unidad académica/domicilio ni forma de refrescarlos sin
 * volver a iniciar sesión. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilModel {
    private String nombre;
    private String correoInstitucional;
    private String boleta;
    private String unidadAcademica;
    private String correoPersonal;
    private String telefonoCelular;
    private String domicilio;
}
