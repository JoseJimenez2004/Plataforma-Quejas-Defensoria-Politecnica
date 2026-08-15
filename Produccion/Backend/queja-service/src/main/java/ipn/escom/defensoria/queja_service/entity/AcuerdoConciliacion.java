package ipn.escom.defensoria.queja_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Acuerdo de conciliación propuesto por la Defensoría (Art. 16 del Acuerdo de creación de la
 * DDP: "La Defensoría tendrá la facultad de proponer a las partes llegar a una conciliación").
 *
 * Se mapea sobre la tabla "acuerdos_conciliacion", que también mapea revision-service con su
 * propia clase (mismo patrón "entidad propia sobre tabla compartida" ya usado con "quejas").
 * Aquí en queja-service esta entidad es de SOLO LECTURA + respuesta del quejoso (aceptar/
 * rechazar); la creación del acuerdo la hace el personal (staff con rol SUBDEFENSOR, DEFENSOR
 * o ADMIN_SISTEMAS) desde revision-service.
 *
 * No se modela como relación JPA hacia Queja (evitaría mezclar entidades de dos módulos
 * distintos sobre la misma fila) -- se referencia por folio y correo, igual que el resto de
 * columnas nuevas que fue agregando revision-service directamente sobre "quejas".
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

    /** Correo del miembro del personal que emitió el acuerdo (para trazabilidad). */
    @Column(name = "creado_por")
    private String creadoPor;
}
