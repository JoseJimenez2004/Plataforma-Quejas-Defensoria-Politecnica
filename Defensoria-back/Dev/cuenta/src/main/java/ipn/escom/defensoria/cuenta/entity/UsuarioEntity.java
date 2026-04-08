package ipn.escom.defensoria.cuenta.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos Personales ---
    @Column(nullable = false)
    private String nombre;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    private Integer edad;

    // --- Identidad Politécnica ---
    @Column(name = "boleta_empleado", unique = true, nullable = false)
    private String boletaEmpleado;

    @Column(name = "unidad_academica")
    private String unidadAcademica;

    // --- Datos de Acceso ---
    @Column(unique = true, nullable = false)
    private String correo;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private String rol = "ROLE_USER";
}