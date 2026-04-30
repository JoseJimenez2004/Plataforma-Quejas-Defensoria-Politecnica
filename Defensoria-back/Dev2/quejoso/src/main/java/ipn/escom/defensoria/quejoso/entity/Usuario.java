package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String correoInstitucional; // @ipn.mx obligatorio

    @Column(unique = true, nullable = false)
    private String boleta; // Identificador institucional

    private String password; // Validado: 8 chars, 1 mayúscula, 1 número
    private String unidadAcademica; // Catálogo fijo (ESCOM, etc.)
    private boolean activo = false;

    private String correoPersonal;
    private String telefonoCelular;

    // Datos del Tutor asociados al perfil del usuario (Contacto de emergencia)
    private String nombreTutor;
    private String parentescoTutor;
    private String telefonoTutor;

    @OneToMany(mappedBy = "quejoso")
    @ToString.Exclude
    private List<Queja> quejas;

    // En tu entidad Usuario.java
    private String codigoRecuperacion;
    private LocalDateTime fechaExpiracionCodigo;
}