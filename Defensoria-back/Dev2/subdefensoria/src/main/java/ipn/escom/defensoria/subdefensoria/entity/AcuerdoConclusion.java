package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TS-07/TS-08 del BPMN: acuerdo y notificacion al quejoso con el que
 * Subdefensoria misma cierra el expediente (sin escalar a
 * Defensoria/Titular) y lo envia para archivo al area secretarial.
 */
@Entity
@Table(name = "acuerdos_conclusion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuerdoConclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false, unique = true)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    /** Texto libre redactado por el abogado asesor (acuerdo + notificacion). */
    @Column(name = "texto_acuerdo", nullable = false, columnDefinition = "TEXT")
    private String textoAcuerdo;

    @Column(name = "ruta_pdf_generado", length = 300)
    private String rutaPdfGenerado;

    @Column(name = "concluido", nullable = false)
    private Boolean concluido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio_secretarial")
    private LocalDateTime fechaEnvioSecretarial;
}
