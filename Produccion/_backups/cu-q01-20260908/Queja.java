package ipn.escom.defensoria.queja_service.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

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

    /** @deprecated ruta en disco de una sola evidencia — así se guardaba antes de que se
     * decidiera soportar varios archivos en la tabla queja_evidencias (ver esa entidad). Se
     * deja el campo/columna para no perder el dato de quejas ya registradas con el esquema
     * anterior, pero el registro de quejas nuevas ya no lo usa. */
    @Deprecated
    @Column(name = "ruta_evidencia")
    private String rutaEvidencia;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // ---- Datos estructurados del quejoso y de la queja ----
    // Antes estos campos se mandaban como texto libre concatenado dentro de "descripcion".
    // Se estructuran como columnas propias para poder filtrar/reportar por ellos y porque el
    // nuevo endpoint público de registro (sin JWT) necesita guardar la identidad completa de
    // quien presenta la queja, no solo su correo institucional.
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

    /** Número de boleta (alumno) o número de empleado, según tipoIdentificacionQuejoso. */
    @Column(name = "numero_identificacion_quejoso")
    private String numeroIdentificacionQuejoso;

    /** Clave de la dependencia (catalogo-service) donde ocurrieron los hechos. */
    @Column(name = "unidad_academica_clave")
    private String unidadAcademicaClave;

    @Column(name = "fecha_hechos")
    private LocalDate fechaHechos;

    @Column(name = "nombre_denunciado")
    private String nombreDenunciado;

    @Column(name = "apellido_denunciado")
    private String apellidoDenunciado;

    /** "AUTENTICADO" (vino de /registrar, con JWT) | "PUBLICO" (vino de /registro-publico). */
    @Column(name = "origen_registro")
    private String origenRegistro;

    /** "RECIBIDA" | "EN_REVISION" | "FINALIZADA" — estatus de trámite. Antes no existía
     * ningún campo de estatus (ver docs/HALLAZGOS.md); se agrega con default "RECIBIDA" para
     * toda queja nueva. Las quejas ya existentes en la BD (creadas antes de este campo)
     * quedan con NULL hasta que se actualicen a mano — el frontend las trata como "RECIBIDA"
     * si llega vacío, para no romper la vista.
     */
    @Column(name = "estatus")
    private String estatus = "RECIBIDA";

    // Columnas nuevas agregadas por revision-service (panel del recepcionista) sobre esta
    // MISMA tabla "quejas" -- se mapean aquí también para que este endpoint público (el que
    // usa "Consultar Queja") le muestre al quejoso el motivo cuando su queja fue rechazada por
    // documentación incompleta, sin tener que llamar a otro microservicio.
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "area_turnada")
    private String areaTurnada;

    @Column(name = "fecha_turnado")
    private LocalDateTime fechaTurnado;

    // Se excluye del JSON de respuesta (@JsonIgnore) para no mandar los archivos completos
    // en bytes cada vez que se consulta una queja — la lista de evidencias (sin el contenido)
    // se expone aparte, en un endpoint propio, cuando se construya.
    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "queja", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuejaEvidencia> evidencias = new ArrayList<>();

    // Datos del tutor/adulto responsable, solo presentes cuando el quejoso es menor de edad.
    @ToString.Exclude
    @OneToOne(mappedBy = "queja", cascade = CascadeType.ALL, orphanRemoval = true)
    private QuejaTutor tutor;
}