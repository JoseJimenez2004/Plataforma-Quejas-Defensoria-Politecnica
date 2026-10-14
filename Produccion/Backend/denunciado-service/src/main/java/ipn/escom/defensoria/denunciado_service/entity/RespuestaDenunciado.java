package ipn.escom.defensoria.denunciado_service.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Respuesta de la persona denunciada en una queja. Tabla respuestas_denunciado en
 * defensoria_db (la crea Hibernate, ddl-auto: update).
 *
 * folio_queja es texto, igual que el resto de enlaces entre microservicios (no hay FK hacia
 * quejas porque esa tabla es de queja-service).
 */
@Entity
@Table(name = "respuestas_denunciado",
       indexes = @Index(name = "idx_respuestas_denunciado_folio_queja", columnList = "folio_queja"))
public class RespuestaDenunciado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Folio propio de esta respuesta: RD-XXXXXXXX. */
    @Column(name = "folio_respuesta", nullable = false, unique = true, length = 20)
    private String folioRespuesta;

    @Column(name = "folio_queja", nullable = false, length = 20)
    private String folioQueja;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido1;

    @Column(length = 50)
    private String apellido2;

    /** Clave del catálogo de dependencias (catalogo-service), p. ej. la de ESCOM. */
    @Column(name = "unidad_procedencia_clave", nullable = false, length = 40)
    private String unidadProcedenciaClave;

    /** ALUMNO | EMPLEADO */
    @Column(name = "tipo_identificacion", nullable = false, length = 20)
    private String tipoIdentificacion;

    /** Boleta o número de empleado; texto porque los ceros a la izquierda importan. */
    @Column(name = "numero_identificacion", nullable = false, length = 10)
    private String numeroIdentificacion;

    @Column(name = "descripcion_hechos", nullable = false, columnDefinition = "TEXT")
    private String descripcionHechos;

    @Column(name = "aviso_privacidad_aceptado", nullable = false)
    private boolean avisoPrivacidadAceptado;

    @Column(name = "aviso_privacidad_version", length = 10)
    private String avisoPrivacidadVersion;

    @Column(name = "aviso_privacidad_fecha")
    private LocalDateTime avisoPrivacidadFecha;

    /** RECIBIDA por ahora; queda para que el personal marque su revisión más adelante. */
    @Column(nullable = false, length = 20)
    private String estatus;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "respuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaDenunciadoArchivo> archivos = new ArrayList<>();

    public void agregarArchivo(RespuestaDenunciadoArchivo archivo) {
        archivo.setRespuesta(this);
        archivos.add(archivo);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolioRespuesta() { return folioRespuesta; }
    public void setFolioRespuesta(String folioRespuesta) { this.folioRespuesta = folioRespuesta; }
    public String getFolioQueja() { return folioQueja; }
    public void setFolioQueja(String folioQueja) { this.folioQueja = folioQueja; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido1() { return apellido1; }
    public void setApellido1(String apellido1) { this.apellido1 = apellido1; }
    public String getApellido2() { return apellido2; }
    public void setApellido2(String apellido2) { this.apellido2 = apellido2; }
    public String getUnidadProcedenciaClave() { return unidadProcedenciaClave; }
    public void setUnidadProcedenciaClave(String clave) { this.unidadProcedenciaClave = clave; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }
    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numero) { this.numeroIdentificacion = numero; }
    public String getDescripcionHechos() { return descripcionHechos; }
    public void setDescripcionHechos(String descripcionHechos) { this.descripcionHechos = descripcionHechos; }
    public boolean isAvisoPrivacidadAceptado() { return avisoPrivacidadAceptado; }
    public void setAvisoPrivacidadAceptado(boolean aceptado) { this.avisoPrivacidadAceptado = aceptado; }
    public String getAvisoPrivacidadVersion() { return avisoPrivacidadVersion; }
    public void setAvisoPrivacidadVersion(String version) { this.avisoPrivacidadVersion = version; }
    public LocalDateTime getAvisoPrivacidadFecha() { return avisoPrivacidadFecha; }
    public void setAvisoPrivacidadFecha(LocalDateTime fecha) { this.avisoPrivacidadFecha = fecha; }
    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public List<RespuestaDenunciadoArchivo> getArchivos() { return archivos; }
}
