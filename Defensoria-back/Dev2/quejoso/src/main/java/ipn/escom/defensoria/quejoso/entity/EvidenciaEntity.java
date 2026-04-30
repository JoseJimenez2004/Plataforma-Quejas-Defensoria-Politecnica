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