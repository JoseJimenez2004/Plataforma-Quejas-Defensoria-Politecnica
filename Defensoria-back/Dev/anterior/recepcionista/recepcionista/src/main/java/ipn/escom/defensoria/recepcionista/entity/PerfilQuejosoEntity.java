package ipn.escom.defensoria.recepcionista.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que mapea la tabla "perfiles_quejoso" de PostgreSQL.
 *
 * Contiene los datos personales del quejoso: nombre, apellidos, boleta
 * y dependencia de origen. Se crea cuando un quejoso se registra en el sistema.
 *
 * Relación con QuejaEntity: un quejoso puede tener múltiples quejas,
 * pero cada queja tiene un solo quejoso (@ManyToOne desde QuejaEntity).
 *
 * PENDIENTE: Agregar fechaNacimiento cuando se integre con el microservicio folio,
 * ya que ese campo viene del formulario del quejoso pero no existe en esta tabla aún.
 */
@Entity
@Table(name = "perfiles_quejoso")
@Data
@NoArgsConstructor
public class PerfilQuejosoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia al usuario en la tabla "usuarios" (credenciales de acceso)
    @Column(name = "usuario_id", unique = true, nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 50)
    private String segundoApellido;

    @Column(name = "es_mayor_edad", nullable = false)
    private Boolean esMayorEdad;

    // Número de boleta (estudiantes) o número de empleado (docentes/administrativos)
    @Column(name = "boleta_empleado", length = 50)
    private String boletaEmpleado;

    // ID de la unidad académica (ESCOM, ENM, etc.) — referencia a cat_dependencias
    @Column(name = "dependencia_id")
    private Integer dependenciaId;

    /**
     * Construye el nombre completo concatenando nombre + apellidos.
     * Usado en los DTOs para mostrar el nombre en el dashboard y validación.
     * El segundo apellido es opcional — solo se agrega si existe.
     */
    public String getNombreCompleto() {
        String completo = nombre + " " + primerApellido;
        if (segundoApellido != null && !segundoApellido.isBlank()) {
            completo += " " + segundoApellido;
        }
        return completo;
    }
}
