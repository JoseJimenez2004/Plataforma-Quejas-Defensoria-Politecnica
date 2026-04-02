package ipn.escom.defensoria.folio.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name= "folios")

public class FolioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String codigoFolio;
    private LocalDateTime fechaCreacion;
    
    public FolioEntity(){}

    public String getCodigoFolio() {
        return codigoFolio;
    }

    public void setCodigoFolio(String codigoFolio) {
        this.codigoFolio = codigoFolio;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }    
}
