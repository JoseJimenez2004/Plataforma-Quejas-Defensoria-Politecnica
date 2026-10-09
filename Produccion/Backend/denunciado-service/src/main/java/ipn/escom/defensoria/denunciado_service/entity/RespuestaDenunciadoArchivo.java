package ipn.escom.defensoria.denunciado_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Credencial o evidencia del denunciado, guardada completa en Postgres (BYTEA). */
@Entity
@Table(name = "respuesta_denunciado_archivos")
public class RespuestaDenunciadoArchivo {

    public static final String TIPO_IDENTIFICACION = "IDENTIFICACION";
    public static final String TIPO_EVIDENCIA = "EVIDENCIA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_id", nullable = false)
    private RespuestaDenunciado respuesta;

    /** IDENTIFICACION | EVIDENCIA */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    /** MIME detectado por firma binaria, no el que mandó el navegador. */
    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    // Sin @Lob a propósito: en Hibernate 6 + Postgres @Lob sobre byte[] se mapea a oid.
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    public Long getId() { return id; }
    public RespuestaDenunciado getRespuesta() { return respuesta; }
    public void setRespuesta(RespuestaDenunciado respuesta) { this.respuesta = respuesta; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }
    public long getTamanioBytes() { return tamanioBytes; }
    public void setTamanioBytes(long tamanioBytes) { this.tamanioBytes = tamanioBytes; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
}
