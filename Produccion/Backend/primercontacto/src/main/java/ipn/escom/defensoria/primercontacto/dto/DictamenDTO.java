package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictamenDTO {

    private Long id;
    private Long expedienteId;
    private String folio;
    private Long analistaId;
    private String analistaNombre;
    private String resultado;
    private String justificacion;
    private String areaTurno;
    private String responsableTurno;
    private String fechaDictamen;
    private String observaciones;

}