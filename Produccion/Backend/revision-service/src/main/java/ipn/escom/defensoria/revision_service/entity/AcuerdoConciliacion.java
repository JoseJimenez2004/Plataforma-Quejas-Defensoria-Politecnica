package ipn.escom.defensoria.revision_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Mismo patrón que Queja/QuejaEvidencia en este servicio: entidad propia sobre la tabla
 * compartida "acuerdos_conciliacion" (también mapeada por queja-service, del lado del
 * quejoso). Aquí, del lado del personal, es donde se CREAN los acuerdos (Art. 16 del Acuerdo
 * de creación de la DDP); el quejoso solo los lee y responde (aceptar/rechazar) desde
 * queja-service.
 */
@Data
@Entity
@Table(name = "acuerdos_conciliacion")
public class AcuerdoConciliacion {

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

    /** "PENDIENTE" | "ACEPTADO" | "RECHAZADO" */
    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "comentario_quejoso", columnDefinition = "TEXT")
    private String comentarioQuejoso;

    @Column(name = "creado_por")
    private String creadoPor;
}
