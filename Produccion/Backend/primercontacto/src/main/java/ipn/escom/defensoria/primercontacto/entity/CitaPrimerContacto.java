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
}