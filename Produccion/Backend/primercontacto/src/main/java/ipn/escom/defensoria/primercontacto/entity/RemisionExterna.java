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

    /** El oficio ya se generó (PDF descargable) pero todavía no se registra su envío. */
    public static final String ESTATUS_GENERADA = "GENERADA";

    /** El analista registró que el oficio se envió a la instancia externa. */
    public static final String ESTATUS_ENVIADA = "ENVIADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, unique = true,length = 50)
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

    /*
     * GENERADA -> ENVIADA. Nullable a propósito: la columna se agrega con ddl-auto=update
     * sobre una tabla que ya tiene filas, y un NOT NULL sin default haría fallar el ALTER.
     * Las filas viejas (null) se crearon con el flujo anterior, que enviaba en el mismo
     * paso, así que se leen como ENVIADA -- ver RemisionExternaService.estatusDe().
     */
    @Column(name = "estatus", length = 20)
    private String estatus;

    /* Número de oficio impreso en el PDF, ej. DDP/PC/REM/2026/0007. */
    @Column(name = "numero_oficio", length = 40)
    private String numeroOficio;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
