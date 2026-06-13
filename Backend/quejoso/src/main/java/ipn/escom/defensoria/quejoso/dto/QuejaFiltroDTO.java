package ipn.escom.defensoria.quejoso.dto;

import ipn.escom.defensoria.quejoso.entity.EstatusQuejaEntity;
import lombok.Data;
import java.time.LocalDate;

@Data
public class QuejaFiltroDTO {
    private String folio;
    private EstatusQuejaEntity estatus;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}