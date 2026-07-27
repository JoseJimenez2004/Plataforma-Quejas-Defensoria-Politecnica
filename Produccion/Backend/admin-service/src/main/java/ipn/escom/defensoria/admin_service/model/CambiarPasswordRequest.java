package ipn.escom.defensoria.admin_service.model;

import lombok.Data;

/** Body de PUT /api/admin/perfil/password -- autoservicio, cualquier rol autenticado cambia
 * SU PROPIA contraseña (a diferencia de PersonalController, que exige ADMIN_SISTEMAS y opera
 * sobre OTROS usuarios). */
@Data
public class CambiarPasswordRequest {
    private String passwordActual;
    private String passwordNueva;
}
