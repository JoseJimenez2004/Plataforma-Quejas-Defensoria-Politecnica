package ipn.escom.defensoria.queja_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Datos del tutor/adulto responsable, capturados en el formulario cuando el quejoso es menor
 * de edad (ver "modal de tutor" en registro-queja-publico). Relación 1 a 1 con Queja — cada
 * queja de un menor tiene, a lo más, un tutor asociado.
 */
@Data
@Entity
@Table(name = "queja_tutores")
public class QuejaTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Excluido de toString/equals/hashCode por la misma razón que en QuejaEvidencia: evitar
    // la recursión infinita Queja <-> QuejaTutor que generaría Lombok @Data por default.
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne
    @JoinColumn(name = "queja_id", nullable = false, unique = true)
    private Queja queja;

    private String nombre;

    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    private String parentesco;

    private String correo;

    private String telefono;
}
