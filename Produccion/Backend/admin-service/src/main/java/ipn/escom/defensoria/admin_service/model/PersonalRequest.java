package ipn.escom.defensoria.admin_service.model;

import ipn.escom.defensoria.admin_service.entity.RolStaff;
import lombok.Data;

/** Usado tanto para crear como para editar personal administrativo — en creación se espera
 * "passwordTemporal"; en edición se ignora si viene vacío. */
@Data
public class PersonalRequest {
    private String nombreCompleto;
    private String numeroEmpleado;
    private String correoInstitucional;
    private RolStaff rol;
    private String passwordTemporal;

    /** Solo aplica al editar: si es true, se le exige cambiar la contraseña en el próximo login. */
    private boolean restablecerPassword;

    /** Solo aplica al editar: "Desactivar cuenta temporalmente" del mockup. */
    private boolean desactivarTemporalmente;
}
