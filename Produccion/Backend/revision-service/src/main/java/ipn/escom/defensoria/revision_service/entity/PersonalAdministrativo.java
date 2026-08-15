package ipn.escom.defensoria.revision_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Espejo de SOLO LECTURA de admin-service/entity/PersonalAdministrativo.java, sobre la MISMA
 * tabla "personal_administrativo" -- se usa únicamente para poblar el combo "Defensor / Abogado
 * Responsable" al turnar una queja (filtrando por rol). Este servicio nunca crea/edita
 * personal; esa gestión es exclusiva del panel de administración (admin-service). */
@Data
@Entity
@Table(name = "personal_administrativo")
public class PersonalAdministrativo {

    @Id
    private Long id;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(name = "correo_institucional")
    private String correoInstitucional;

    @Enumerated(EnumType.STRING)
    private RolStaff rol;

    private boolean activo;
}
