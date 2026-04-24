package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tutores")
@Data
public class Tutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String PrimerApellido;
    private String SegundoApellido;
    private String parentesco;
    private String correo;
    private String telefono;
    private String urlIdentificacion; // Opcional según mockup MQ-03

    @OneToOne
    @JoinColumn(name = "queja_id")
    private Queja queja;
}