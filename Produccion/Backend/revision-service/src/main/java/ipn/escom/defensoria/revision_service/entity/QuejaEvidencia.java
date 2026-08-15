package ipn.escom.defensoria.revision_service.entity;

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

/** Espejo de queja-service/entity/QuejaEvidencia.java sobre la MISMA tabla
 * "queja_evidencias" -- se usa tanto para mostrar/descargar los documentos adjuntos de una
 * queja web en "Validación de Requisitos", como para guardar el archivo escaneado del
 * Registro Manual. */
@Data
@Entity
@Table(name = "queja_evidencias")
public class QuejaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida = LocalDateTime.now();
}
