package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejosoResumenRequest {
    private String nombreCompleto;
    private String correo;
    private String unidadAcademica;
}