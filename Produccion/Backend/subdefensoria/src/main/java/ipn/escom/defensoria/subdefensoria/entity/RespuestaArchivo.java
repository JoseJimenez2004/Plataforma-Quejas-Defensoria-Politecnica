package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Archivo de la respuesta del director a un oficio (CU-SD-08): el oficio de respuesta
 * escaneado o en PDF. Se guarda completo en Postgres (bytea), igual que las evidencias de la
 * queja, y en su propia tabla para que listar respuestas no cargue los archivos.
 */
@Entity
@Table(name = "respuestas_externas_archivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "respuesta_id", nullable = false, unique = true)
    private Long respuestaId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    /* application/pdf, image/jpeg o image/png, según la firma real del archivo. */
    @Column(name = "tipo_contenido", nullable = false, length = 50)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    // Sin @Lob a propósito: en Hibernate 6 + Postgres, @Lob sobre byte[] mapea a "oid"; con
    // columnDefinition explícito queda como bytea (mismo criterio que QuejaEvidencia).
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;
}
