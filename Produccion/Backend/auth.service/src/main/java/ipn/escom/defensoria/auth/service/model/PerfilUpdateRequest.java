package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

/** Body de PUT /api/auth/perfil -- solo los campos que el propio quejoso puede editar
 * (nombre/correo institucional/boleta son de solo lectura, vienen de la activación de cuenta). */
@Data
public class PerfilUpdateRequest {
    private String correoPersonal;
    private String telefonoCelular;
    private String unidadAcademica;
    private String domicilio;
}
