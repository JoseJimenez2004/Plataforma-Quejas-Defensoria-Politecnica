package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Espejo de SOLO LECTURA de la tabla "personal_administrativo".
 *
 * La administración de usuarios sigue siendo responsabilidad
 * exclusiva de admin-service.
 *
 * Primer Contacto únicamente consulta esta tabla para identificar
 * al analista autenticado a partir del correo contenido en el JWT.
 */
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