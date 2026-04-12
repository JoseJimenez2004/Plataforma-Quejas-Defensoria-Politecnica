package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "expedientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String folio;

    @Column(nullable = false)
    private String nombreQuejoso;

    @Column(nullable = false)
    private String correoQuejoso;

    private String boleta;

    @Lob
    private String narrativa;

    private String prioridad; // Alta, Media, Baja

    private String tipoViolacion;

    private String estatus; // PENDIENTE, ADMITIDO, REMITIDO

    private String motivoRemision;

    private LocalDateTime fechaIngreso;
}