package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un expediente ya admitido por Primer Contacto (acuerdo
 * de admision, act. 10 del DDP-PO-02) que entra a la etapa de
 * investigacion a cargo de la Subdefensoria (Abogado Asesor),
 * actividades 11 a 15 del procedimiento.
 *
 * A diferencia de Primer Contacto (que solo mantiene un espejo en
 * memoria de datos que no le pertenecen), aqui SI persistimos: la
 * Subdefensoria es la fuente de verdad de todo lo que pasa durante
 * la investigacion (oficios, plazos, dictamen).
 */
@Entity
@Table(name = "expedientes_investigacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteInvestigacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queja_id", nullable = false, unique = true)
    private Long quejaId;

    @Column(name = "folio", nullable = false, unique = true, length = 50)
    private String folio;

    @Column(name = "quejoso_nombre", length = 150)
    private String quejosoNombre;

    @Column(name = "unidad_academica", length = 100)
    private String unidadAcademica;

    @Column(name = "asunto", length = 200)
    private String asunto;

    @Column(name = "descripcion_hechos", columnDefinition = "TEXT")
    private String descripcionHechos;

    @Column(name = "fecha_admision", nullable = false)
    private LocalDate fechaAdmision;

    @Column(name = "abogado_asesor_id")
    private Long abogadoAsesorId;

    @Column(name = "abogado_asesor_nombre", length = 150)
    private String abogadoAsesorNombre;

    /**
     * RECIBIDO -> EN_INVESTIGACION -> EN_GESTION_DIRECTOR ->
     * LISTO_A_DICTAMINAR -> CONCLUIDO
     * Ver EstatusExpediente para el detalle de cada valor.
     */
    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus;

    @Column(name = "observaciones_analista", columnDefinition = "TEXT")
    private String observacionesAnalista;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
