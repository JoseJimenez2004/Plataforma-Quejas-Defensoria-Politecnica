package ipn.escom.defensoria.consulta.dto;

import java.util.List;
import lombok.Data;

@Data
public class ConsultaDTO {

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

    public int getProgreso() {
        return progreso;
    }

    public void setProgreso(int progreso) {
        this.progreso = progreso;
    }

    public List<String> getListaEvidencias() {
        return listaEvidencias;
    }

    public void setListaEvidencias(List<String> listaEvidencias) {
        this.listaEvidencias = listaEvidencias;
    }
    
    private List<HitoDetalleDTO> lineaTiempo;

    @Data
    public static class HitoDetalleDTO {
        private String evento;
        private String fechaHora;
    }
    private String folio;
    private String fechaInicio;
    private String asunto;
    private String status;
    private int progreso;
    private List<String> listaEvidencias; 
}
