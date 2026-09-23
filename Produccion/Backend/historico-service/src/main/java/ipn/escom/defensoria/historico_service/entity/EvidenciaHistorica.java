package ipn.escom.defensoria.historico_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Archivo digitalizado de una queja histórica (el escaneo del oficio, la credencial, el
 * acuerdo). Mismo criterio que queja_evidencias en defensoria_db: el binario se guarda en
 * Postgres como bytea para que el respaldo de la base se lleve también los archivos.
 */
@Data
@Entity
@Table(name = "quejas_historicas_evidencias")
public class EvidenciaHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Excluido de toString/equals/hashCode para evitar la recursión infinita de la relación
    // bidireccional (mismo motivo que en QuejaEvidencia de queja-service).
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "queja_historica_id", nullable = false)
    private QuejaHistorica quejaHistorica;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    /** "IDENTIFICACION" | "EVIDENCIA" | "ACUERDO" | "OFICIO" */
    @Column(length = 20)
    private String tipo;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    // Sin @Lob a propósito: en Hibernate 6 + Postgres, @Lob sobre byte[] mapea a "oid"
    // (Large Object) en vez de "bytea". Declarándolo así va directo a bytea.
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida = LocalDateTime.now();
}
