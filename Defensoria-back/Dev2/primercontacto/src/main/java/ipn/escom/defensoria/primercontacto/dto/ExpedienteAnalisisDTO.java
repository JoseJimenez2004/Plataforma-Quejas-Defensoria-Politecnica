package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteAnalisisDTO {

    private Long quejaId;
    private String folio;
    private String descripcionHechos;
    private String fechaRecepcion;
    private String estatus;
    private String prioridad;
    private QuejosoDTO quejoso;
    private List<EvidenciaDTO> evidencias;
    private List<NotaAnalisisDTO> notas;
}