package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "remisiones_externas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemisionExterna {
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

    @Column(name = "autoridad_remision", nullable = false, length = 200)
    private String autoridadRemision;

    @Column(name = "justificacion_legal", nullable = false, columnDefinition = "TEXT")
    private String justificacionLegal;

    @Column(name = "sugerencia_quejoso", columnDefinition = "TEXT")
    private String sugerenciaQuejoso;

    @Column(name = "adjuntar_expediente", nullable = false)
    private Boolean adjuntarExpediente;

    @Column(name = "fecha_remision", nullable = false)
    private LocalDateTime fechaRemision;
}