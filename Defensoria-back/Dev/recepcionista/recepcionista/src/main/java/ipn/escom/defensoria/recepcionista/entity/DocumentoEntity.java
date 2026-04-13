package ipn.escom.defensoria.recepcionista.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla "documentos" de PostgreSQL.
 *
 * Representa un archivo adjunto vinculado a una queja. Cada queja puede
 * tener múltiples documentos (credencial, evidencias fotográficas, escritos, etc.).
 *
 * En la pantalla de Validación de Requisitos, la recepcionista ve la lista
 * de nombres de archivos adjuntos para verificar que la documentación esté completa.
 *
 * El archivo físico NO se almacena en la BD — solo los metadatos y la URL
 * donde está guardado (urlAlmacenamiento). En producción, esto apuntaría
 * a un servicio de almacenamiento como S3 o un servidor de archivos local.
 *
 * hashSha256: huella digital del archivo para verificar integridad.
 * Si el hash cambia, el archivo fue modificado.
 */
@Entity
@Table(name = "documentos")
@Data
@NoArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID de la queja a la que pertenece este documento
    @Column(name = "queja_id", nullable = false)
    private Long quejaId;

    // Nombre original del archivo tal como lo subió el quejoso (ej. "Declaracion_Escrita.docx")
    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    // Tipo MIME del archivo (ej. "application/pdf", "image/jpeg")
    @Column(name = "tipo_archivo", nullable = false, length = 50)
    private String tipoArchivo;

    // Tamaño del archivo en bytes
    @Column(name = "peso_bytes", nullable = false)
    private Long pesoBytes;

    // Hash SHA-256 del archivo para verificación de integridad
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    // Ruta o URL donde está almacenado el archivo físico
    @Column(name = "url_almacenamiento", nullable = false, columnDefinition = "TEXT")
    private String urlAlmacenamiento;

    // Sello digital opcional para documentos firmados electrónicamente
    @Column(name = "sello_digital", columnDefinition = "TEXT")
    private String selloDigital;

    // ID del usuario que subió el documento
    @Column(name = "usuario_subio_id", nullable = false)
    private Integer usuarioSubioId;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;
}
