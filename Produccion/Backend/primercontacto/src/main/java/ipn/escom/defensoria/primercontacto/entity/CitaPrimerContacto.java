package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "citas_primer_contacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaPrimerContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "quejoso_id")
    private Long quejosoId;

    @Column(name = "quejoso_nombre", length = 150)
    private String quejosoNombre;

    @Column(name = "analista_id", nullable = false)
    private Long analistaId;

    @Column(name = "analista_nombre", length = 150)
    private String analistaNombre;

    @Column(name = "fecha_cita", nullable = false)
    private LocalDate fechaCita;

    @Column(name = "hora_cita", nullable = false)
    private LocalTime horaCita;

    @Column(name = "tipo_cita", nullable = false, length = 50)
    private String tipoCita;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "estatus", nullable = false, length = 50)
    private String estatus;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /*
     * Último analista que confirmó, reagendó o canceló la cita (CU-PC-04):
     * quien la agenda queda en analistaId/analistaNombre, quien la movió después, aquí.
     */
    @Column(name = "actualizado_por_id")
    private Long actualizadoPorId;

    @Column(name = "actualizado_por_nombre", length = 150)
    private String actualizadoPorNombre;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /*
     * Respuesta del quejoso: al agendar o reagendar tiene un plazo (48 h por defecto) para
     * confirmar o cancelar con motivo. Si no responde, la cita pasa a SIN_RESPUESTA.
     * Las citas anteriores a este cambio quedan con fechaLimiteRespuesta en null y no vencen.
     */
    @Column(name = "fecha_limite_respuesta")
    private LocalDateTime fechaLimiteRespuesta;

    @Column(name = "fecha_respuesta_quejoso")
    private LocalDateTime fechaRespuestaQuejoso;

    @Column(name = "motivo_cancelacion_quejoso", columnDefinition = "TEXT")
    private String motivoCancelacionQuejoso;

    /* QUEJOSO si respondió desde su panel; ANALISTA si el analista registró su respuesta. */
    @Column(name = "respuesta_registrada_por", length = 20)
    private String respuestaRegistradaPor;
}