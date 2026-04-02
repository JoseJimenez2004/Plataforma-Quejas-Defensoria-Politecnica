package ipn.escom.defensoria.consulta.entity;

import jakarta.persistence.*;

@Entity
@Table(name="quejas")

public class ConsultaEntity {
    @Id
    private String folio;
    private String fechaInicio;
    private String asunto;
    private String status;

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    
}
