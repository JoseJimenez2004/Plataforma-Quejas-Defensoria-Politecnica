package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejosoResumenDTO {

    private String nombreCompleto;
    private String correo;
    private String unidadAcademica;
}
