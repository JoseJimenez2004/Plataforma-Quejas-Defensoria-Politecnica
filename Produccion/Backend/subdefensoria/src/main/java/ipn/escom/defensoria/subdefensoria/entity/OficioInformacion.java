package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Oficio redactado por el abogado asesor (texto libre), dirigido a
 * un destinatario (director de escuela/departamento) por correo.
 * La generacion real del PDF y el envio de correo quedan como campos
 * preparados (rutaPdfGenerado, correoEnviado) para conectar despues;
 * por ahora solo se persiste el contenido y el destinatario.
 *
 * fase distingue TS-01/02/03 (SOLICITUD_INFORMACION) de TS-04/05/06
 * (GESTION_DIRECTOR), cada una con su propio ciclo de espera/recordatorio.
 */
@Entity
@Table(name = "oficios_informacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OficioInformacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "numero_oficio", nullable = false, length = 50)
    private String numeroOficio;

    /** SOLICITUD_INFORMACION o GESTION_DIRECTOR. */
    @Column(name = "fase", nullable = false, length = 30)
    private String fase;

    @Column(name = "destinatario_nombre", nullable = false, length = 200)
    private String destinatarioNombre;

    @Column(name = "destinatario_correo", nullable = false, length = 150)
    private String destinatarioCorreo;

    @Column(name = "unidad_academica", length = 100)
    private String unidadAcademica;

    /** Texto libre redactado por el abogado asesor. */
    @Column(name = "contenido_redactado", nullable = false, columnDefinition = "TEXT")
    private String contenidoRedactado;

    /** Preparado para cuando se conecte la generacion real de PDF/correo. */
    @Column(name = "ruta_pdf_generado", length = 300)
    private String rutaPdfGenerado;

    @Column(name = "correo_enviado", nullable = false)
    @Builder.Default
    private Boolean correoEnviado = false;

    /** PRIMERA (10 dias habiles) o SUBSECUENTE (5 dias habiles). */
    @Column(name = "tipo_plazo", nullable = false, length = 20)
    private String tipoPlazo;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDate fechaEnvio;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    /** EN_ESPERA, VENCIDO o RESPONDIDO. */
    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
