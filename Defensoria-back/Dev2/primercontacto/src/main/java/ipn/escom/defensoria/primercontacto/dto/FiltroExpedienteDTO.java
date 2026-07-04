package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltroExpedienteDTO {

    private String folio;
    private String nombreQuejoso;
    private String estatus;
    private String unidadAcademica;
    private String prioridad;
    private String fechaInicio;
    private String fechaFin;
}