package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.Entity;

import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Data;

@Entity
@Table(name = "evidencias", schema = "defensoria")
@Data
public class EvidenciaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String urlAlmacenamiento; // Clave retornada por StorageService (ruta relativa o key de nube)
    private Long tamano; // Para validar los 30MB totales
    private String tipoContenido; // MIME type, ej. "application/pdf", "image/jpeg"

    @ManyToOne
    @JoinColumn(name = "queja_id")
    private Queja queja;
}