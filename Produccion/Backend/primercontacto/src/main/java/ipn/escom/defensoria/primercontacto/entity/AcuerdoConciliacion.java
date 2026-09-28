package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Misma tabla compartida "acuerdos_conciliacion" que ya usan revision-service (emite) y
 * queja-service (el quejoso acepta/rechaza desde su panel). CU-PC-10 la reutiliza en vez de
 * crear una tabla propia: así el quejoso ve el acuerdo en la misma pantalla de siempre.
 *
 * numeroFolio es el folio de la QUEJA (FOL-...), no el folio PC-...: es con el que el
 * quejoso identifica su trámite.
 */
@Getter
@Setter
@Entity
@Table(name = "acuerdos_conciliacion")
public class AcuerdoConciliacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADO = "ACEPTADO";
    public static final String RECHAZADO = "RECHAZADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_folio", nullable = false)
    private String numeroFolio;

    @Column(name = "correo_institucional", nullable = false)
    private String correoInstitucional;

    @Column(nullable = false)
    private String asunto;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String terminos;

    @Column(nullable = false)
    private String estado = PENDIENTE;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "comentario_quejoso", columnDefinition = "TEXT")
    private String comentarioQuejoso;

    /** Correo institucional de quien emitió el acuerdo. */
    @Column(name = "creado_por")
    private String creadoPor;
}
