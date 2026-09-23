package ipn.escom.defensoria.historico_service.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Una queja que la Defensoría atendió ANTES de que existiera el sistema, capturada a mano
 * por Recepción desde sus Excel, oficios físicos, notas de llamadas o sistemas anteriores.
 *
 * Vive en defensoria_historico_db, una base distinta de defensoria_db: estas quejas son de
 * solo consulta y no entran al flujo (no se validan, no se turnan, no se dictaminan). Su
 * único propósito es que la búsqueda de antecedentes encuentre reincidencia de un quejoso o
 * de un denunciado en años previos. Por eso no tiene estatus de trámite ni folio de Primer
 * Contacto/Subdefensoría.
 */
@Data
@Entity
@Table(name = "quejas_historicas")
public class QuejaHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El folio del expediente ORIGINAL, tal como venía ("DDP/2019/0087", "2019-087"). No se
     * normaliza ni se reasigna: es el número con el que la Defensoría lo ha buscado siempre.
     */
    @Column(nullable = false, unique = true, length = 80)
    private String folio;

    /** true cuando el expediente no traía folio y el sistema generó uno ("SF-nnnnnn"). */
    @Column(name = "folio_generado", nullable = false)
    private Boolean folioGenerado = false;

    /** EXCEL | LLAMADA | OFICIO_FISICO | SISTEMA_ANTERIOR | OTRO */
    @Column(nullable = false, length = 30)
    private String fuente;

    /**
     * Cuándo se presentó la queja ante la Defensoría en su momento. Es la que se expone como
     * "fechaRegistro" en el contrato de antecedentes: si se expusiera la fecha de captura,
     * todos los históricos parecerían de 2026 y el orden temporal quedaría inservible.
     */
    @Column(name = "fecha_presentacion_original")
    private LocalDate fechaPresentacionOriginal;

    @Column(name = "fecha_hechos")
    private LocalDate fechaHechos;

    @Column(name = "unidad_academica_clave", length = 20)
    private String unidadAcademicaClave;

    @Column(length = 255)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** RECHAZADA | IMPROCEDENTE | REMITIDA | CONCLUIDA */
    @Column(nullable = false, length = 40)
    private String estatus = "CONCLUIDA";

    /** SIN_DATO es un valor legítimo y frecuente: el archivo original rara vez dice cómo terminó. */
    @Column(nullable = false, length = 40)
    private String resultado = "SIN_DATO";

    // ------------------------------------------------------------------ quejoso
    @Column(name = "quejoso_tipo_identificacion", length = 20)
    private String quejosoTipoIdentificacion;

    @Column(name = "quejoso_numero_identificacion", length = 50)
    private String quejosoNumeroIdentificacion;

    @Column(name = "quejoso_nombre", length = 150)
    private String quejosoNombre;

    @Column(name = "quejoso_apellido1", length = 150)
    private String quejosoApellido1;

    @Column(name = "quejoso_apellido2", length = 150)
    private String quejosoApellido2;

    @Column(name = "quejoso_tipo_usuario", length = 20)
    private String quejosoTipoUsuario;

    // -------------------------------------------------------------- denunciado
    @Column(name = "denunciado_tipo_identificacion", length = 20)
    private String denunciadoTipoIdentificacion;

    @Column(name = "denunciado_numero_identificacion", length = 50)
    private String denunciadoNumeroIdentificacion;

    @Column(name = "denunciado_nombre", length = 150)
    private String denunciadoNombre;

    @Column(name = "denunciado_apellido1", length = 150)
    private String denunciadoApellido1;

    @Column(name = "denunciado_apellido2", length = 150)
    private String denunciadoApellido2;

    @Column(name = "denunciado_tipo_usuario", length = 20)
    private String denunciadoTipoUsuario;

    // --------------------------------------------------- trazabilidad de captura
    // Interna: NO se expone en el contrato que consume el modelo de antecedentes.
    @Column(name = "capturado_por", length = 150)
    private String capturadoPor;

    @Column(name = "fecha_captura", nullable = false)
    private LocalDateTime fechaCaptura = LocalDateTime.now();

    @Column(name = "notas_captura", columnDefinition = "TEXT")
    private String notasCaptura;

    @OneToMany(mappedBy = "quejaHistorica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvidenciaHistorica> evidencias = new ArrayList<>();

    public void agregarEvidencia(EvidenciaHistorica evidencia) {
        evidencia.setQuejaHistorica(this);
        this.evidencias.add(evidencia);
    }
}
