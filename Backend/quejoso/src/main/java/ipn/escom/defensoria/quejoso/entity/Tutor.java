package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Table(name = "tutores", schema = "defensoria")
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