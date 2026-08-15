package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Recordatorio sobre un oficio (TS-03 en el ciclo de investigacion,
 * TS-06 en el ciclo de gestion con el director). En TS-06 el
 * recordatorio ademas ofrece medidas; como aun no existe el catalogo
 * institucional, medidasOfrecidas es texto libre opcional.
 */
@Entity
@Table(name = "recordatorios_urgencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordatorioUrgencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oficio_id", nullable = false)
    private Long oficioId;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    /** Texto libre por ahora; en TS-06 aqui se describen las medidas ofrecidas. */
    @Column(name = "medidas_ofrecidas", columnDefinition = "TEXT")
    private String medidasOfrecidas;

    @Column(name = "dias_retraso", nullable = false)
    private Integer diasRetraso;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
}
