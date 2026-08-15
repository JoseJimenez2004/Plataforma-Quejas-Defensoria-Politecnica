package ipn.escom.defensoria.revision_service.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

/**
 * MISMA tabla "quejas" que ya usa queja-service (misma base de datos compartida, decisión
 * explícita: revision-service tiene su propia entidad JPA en vez de llamar por REST a
 * queja-service, igual patrón que ya usan varios microservicios sobre defensoria_db).
 *
 * Los primeros campos son un espejo exacto de queja-service/entity/Queja.java (no se deben
 * tocar sin actualizar también allá). Los campos debajo de "Flujo de revisión" son NUEVOS,
 * agregados por este servicio -- con ddl-auto=update, Hibernate hace el ALTER TABLE la primera
 * vez que este servicio arranca contra la base de producción.
 */
@Data
@Entity
@Table(name = "quejas")
public class Queja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_folio", unique = true, nullable = false)
    private String numeroFolio;

    @Column(name = "correo_institucional", nullable = false)
    private String correoInstitucional;

    @Column(nullable = false)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "nombre_quejoso")
    private String nombreQuejoso;

    @Column(name = "apellido_paterno_quejoso")
    private String apellidoPaternoQuejoso;

    @Column(name = "apellido_materno_quejoso")
    private String apellidoMaternoQuejoso;

    @Column(name = "fecha_nacimiento_quejoso")
    private LocalDate fechaNacimientoQuejoso;

    /** "alumno" | "empleado" */
    @Column(name = "tipo_identificacion_quejoso")
    private String tipoIdentificacionQuejoso;

    @Column(name = "numero_identificacion_quejoso")
    private String numeroIdentificacionQuejoso;

    @Column(name = "unidad_academica_clave")
    private String unidadAcademicaClave;

    @Column(name = "fecha_hechos")
    private LocalDate fechaHechos;

    @Column(name = "nombre_denunciado")
    private String nombreDenunciado;

    @Column(name = "apellido_denunciado")
    private String apellidoDenunciado;

    /** "AUTENTICADO" | "PUBLICO" | "MANUAL" (este último lo agrega revision-service cuando el
     * recepcionista da de alta un documento físico recibido en papel). */
    @Column(name = "origen_registro")
    private String origenRegistro;

    /** "RECIBIDA" | "EN_VALIDACION" | "RECHAZADA" | "TURNADA" — ver RevisionQuejaService. */
    @Column(name = "estatus")
    private String estatus = "RECIBIDA";

    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "queja", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuejaEvidencia> evidencias = new ArrayList<>();

    // ---------------- Flujo de revisión (recepcionista) ----------------

    /** Texto combinado de los motivos marcados + observaciones libres, para mostrarlo tal
     * cual tanto en el panel de revisión como en la consulta pública del quejoso. */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "area_turnada")
    private String areaTurnada;

    @Column(name = "defensor_asignado")
    private String defensorAsignado;

    @Column(name = "comentarios_recepcion", columnDefinition = "TEXT")
    private String comentariosRecepcion;

    /** Correo institucional de quien procesó (aprobó/rechazó) la queja. */
    @Column(name = "validado_por")
    private String validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "fecha_turnado")
    private LocalDateTime fechaTurnado;

    // ---- Datos exclusivos del Registro Manual (documento físico) ----

    @Column(name = "numero_oficio")
    private String numeroOficio;

    @Column(name = "fecha_recepcion_fisica")
    private LocalDate fechaRecepcionFisica;

    /** "IDENTIFICACION" | "OFICIO" | "ESCRITO_LIBRE" | "OTRO" */
    @Column(name = "tipo_documento_fisico")
    private String tipoDocumentoFisico;

    @Column(name = "ubicacion_fisica_expediente")
    private String ubicacionFisicaExpediente;

    /** "alumno" | "empleado" | "externo" -- registro manual no siempre sabe boleta/empleado
     * de antemano, a diferencia del registro público del propio quejoso. */
    @Column(name = "tipo_usuario_manual")
    private String tipoUsuarioManual;
}
