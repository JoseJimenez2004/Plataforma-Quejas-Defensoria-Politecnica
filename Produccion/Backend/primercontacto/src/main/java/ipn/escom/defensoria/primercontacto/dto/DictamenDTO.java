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

    /*
     * Para la pantalla de consulta (CU-PC-08): en qué quedó el expediente
     * y, si fue procedente, si Subdefensoría ya lo recibió.
     */
    private String estatusExpediente;
    private String folioSubdefensoria;

}