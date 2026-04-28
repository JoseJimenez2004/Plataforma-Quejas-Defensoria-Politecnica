package ipn.escom.defensoria.consulta.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "queja_hitos")
@Data
@NoArgsConstructor
public class HitoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion; 
    private LocalDateTime fechaHito;

    @ManyToOne
    @JoinColumn(name = "folio_id")
    private ConsultaEntity consulta;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHito() {
        return fechaHito;
    }

    public void setFechaHito(LocalDateTime fechaHito) {
        this.fechaHito = fechaHito;
    }

    public ConsultaEntity getConsulta() {
        return consulta;
    }

    public void setConsulta(ConsultaEntity consulta) {
        this.consulta = consulta;
    }
    
    
}