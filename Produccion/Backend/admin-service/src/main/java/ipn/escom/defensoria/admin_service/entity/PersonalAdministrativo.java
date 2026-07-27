package ipn.escom.defensoria.admin_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Cuenta de personal administrativo/staff (recepcionista, analista de primer contacto,
 * subdefensor, defensor, admin de sistemas) — NO es la misma tabla que "usuarios" de
 * auth.service (esa es para quejosos: tiene boleta, datos de tutor, etc. que no aplican aquí).
 * La crea/gestiona un Admin_Sistemas desde este panel.
 */
@Data
@Entity
@Table(name = "personal_administrativo")
public class PersonalAdministrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "numero_empleado", nullable = false, unique = true)
    private String numeroEmpleado;

    @Column(name = "correo_institucional", nullable = false, unique = true)
    private String correoInstitucional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RolStaff rol;

    @Column(nullable = false)
    private String password;

    /** true si la contraseña fue asignada por el admin y el usuario todavía no ha iniciado
     * sesión con ella por primera vez (se muestra como "cuenta temporal" en la lista). */
    @Column(name = "cuenta_temporal", nullable = false)
    private boolean cuentaTemporal = true;

    /** Si es true, en el próximo login se le exige cambiar la contraseña antes de continuar. */
    @Column(name = "forzar_cambio_password", nullable = false)
    private boolean forzarCambioPassword = true;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;
}
