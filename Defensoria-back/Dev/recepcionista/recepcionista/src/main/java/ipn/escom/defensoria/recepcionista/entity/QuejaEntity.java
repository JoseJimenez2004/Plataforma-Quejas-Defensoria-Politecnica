package ipn.escom.defensoria.recepcionista.entity;

import ipn.escom.defensoria.recepcionista.entity.cat.CatEstatusQueja;
import ipn.escom.defensoria.recepcionista.entity.cat.CatTema;
import ipn.escom.defensoria.recepcionista.entity.cat.CatTipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla "quejas" de PostgreSQL.
 *
 * Representa una queja registrada en el sistema. Es la entidad central
 * del módulo recepcionista — todas las operaciones (aceptar, rechazar,
 * canalizar) modifican el estatus de esta entidad.
 *
 * Relaciones:
 * - estatus → cat_estatus_queja (RECIBIDA, EN_REVISION, ATENDIDA)
 * - tema    → cat_tema (tipo de problemática reportada)
 * - tipoDocumento → cat_tipo_documento
 * - quejoso → perfiles_quejoso (datos personales del quejoso)
 *
 * @ManyToOne con FetchType.LAZY: los objetos relacionados (estatus, tema, etc.)
 * se cargan de la BD solo cuando se accede a ellos, no al cargar la queja.
 * Esto mejora el rendimiento evitando JOINs innecesarios.
 *
 * PENDIENTE: Cuando folio migre a PostgreSQL, agregar campos:
 * asunto, fechaHechos, correoQuejoso, fechaNacimiento, y datos del tutor.
 */
@Entity
@Table(name = "quejas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Folio temporal asignado al registrar (ej. TEMP-001), luego se convierte en no_expediente
    @Column(name = "no_folio", unique = true, length = 50)
    private String noFolio;

    @Column(name = "no_expediente", unique = true, length = 100)
    private String noExpediente;

    private Integer turno;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    @Column(name = "num_fojas")
    private Integer numFojas;

    @Column(name = "fecha_revision_antecedentes")
    private LocalDateTime fechaRevisionAntecedentes;

    // Descripción libre que escribe el quejoso al momento de registrar su queja
    @Column(name = "descripcion_hechos", nullable = false, columnDefinition = "TEXT")
    private String descripcionHechos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private CatTipoDocumento tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id", nullable = false)
    private CatTema tema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estatus_id", nullable = false)
    private CatEstatusQueja estatus;

    // ID del usuario que capturó la queja (recepcionista o sistema)
    @Column(name = "usuario_captura_id", nullable = false)
    private Integer usuarioCapturaId;

    // Relación con el perfil del quejoso que interpuso la queja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quejoso_id", nullable = false)
    private PerfilQuejosoEntity quejoso;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Se actualiza cada vez que cambia el estatus
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
