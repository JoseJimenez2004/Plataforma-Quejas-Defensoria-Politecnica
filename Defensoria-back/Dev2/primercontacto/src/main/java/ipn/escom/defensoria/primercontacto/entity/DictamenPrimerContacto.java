package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dictamenes_primer_contacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictamenPrimerContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queja_id", nullable = false)
    private Long quejaId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "analista_id", nullable = false)
    private Long analistaId;

    @Column(name = "analista_nombre", length = 150)
    private String analistaNombre;

    @Column(name = "resultado", nullable = false, length = 50)
    private String resultado;

    @Column(name = "justificacion", nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "area_turno", length = 150)
    private String areaTurno;

    @Column(name = "responsable_turno", length = 150)
    private String responsableTurno;

    @Column(name = "fecha_dictamen", nullable = false)
    private LocalDateTime fechaDictamen;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}