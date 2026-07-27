package ipn.escom.defensoria.admin_service.model;

import ipn.escom.defensoria.admin_service.entity.RolStaff;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonalResumenModel {
    private Long id;
    private String nombreCompleto;
    private String numeroEmpleado;
    private String correoInstitucional;
    private RolStaff rol;
    private boolean activo;
    private boolean cuentaTemporal;
}
