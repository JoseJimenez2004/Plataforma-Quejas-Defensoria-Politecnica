package ipn.escom.defensoria.consulta.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="quejas")

public class ConsultaEntity {
    @Id
    private String folio;
    private String fechaInicio;
    private String asunto;
    private String status;
    
    @Column(length = 1000) 
    private String evidencias; 
    
    @OneToMany(mappedBy = "consulta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHito ASC") 
    private List<HitoEntity> historial = new ArrayList<>();

    public List<HitoEntity> getHistorial() {
        return historial;
    }

    public void setHistorial(List<HitoEntity> historial) {
        this.historial = historial;
    }
    
    public String getEvidencias() { return evidencias; }
    public void setEvidencias(String evidencias) { this.evidencias = evidencias; }

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
