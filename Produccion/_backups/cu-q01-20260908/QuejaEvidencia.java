package ipn.escom.defensoria.queja_service.entity;

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
 * Un archivo de evidencia adjunto a una queja. Se decidió guardar el contenido binario
 * completo dentro de Postgres (columna BYTEA vía @Lob) en vez de en disco + solo la ruta en
 * la BD — decisión explícita del usuario, con el trade-off conocido (la base de datos crece
 * más rápido y no es el patrón "de libro" para archivos pesados, pero simplifica backups y
 * mudanzas de servidor). Ver docs/CAMBIOS.md para el detalle de la decisión.
 */
@Data
@Entity
@Table(name = "queja_evidencias")
public class QuejaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Excluido de toString/equals/hashCode: Queja también referencia esta lista (relación
    // bidireccional), y Lombok @Data por default incluiría ambos lados, causando una
    // recursión infinita (Queja.toString() -> QuejaEvidencia.toString() -> Queja.toString()...).
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "queja_id", nullable = false)
    private Queja queja;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    // OJO: a propósito NO se usa @Lob aquí. En Hibernate 6 + Postgres, @Lob sobre un byte[]
    // mapea por default al tipo "oid" (Large Object) en vez de "bytea", que es un mecanismo
    // distinto y más difícil de administrar (tabla aparte pg_largeobject, requiere manejo de
    // transacciones especial). Declarando el campo así, sin @Lob, con columnDefinition
    // explícito, Hibernate lo mapea directo a "bytea", que es lo que realmente queremos.
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida = LocalDateTime.now();
}
