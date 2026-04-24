package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "evidencias")
@Data
public class EvidenciaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String urlAlmacenamiento; // Link al servicio de storage
    private Long tamano; // Para validar los 30MB totales

    @ManyToOne
    @JoinColumn(name = "queja_id")
    private Queja queja;
}