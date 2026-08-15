package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandejaAnalisisDTO {

    private Long quejaId;
    private String folio;
    private String nombreQuejoso;
    private String unidadAcademica;
    private String tema;
    private String prioridad;
    private String estatus;
    private String fechaRecepcion;
}