package ipn.escom.defensoria.queja_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "quejas")
public class Queja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_folio", unique = true, nullable = false)
    private String numeroFolio;

    @Column(name = "correo_institucional", nullable = false)
    private String correoInstitucional;

    @Column(nullable = false)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ruta_evidencia")
    private String rutaEvidencia;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}