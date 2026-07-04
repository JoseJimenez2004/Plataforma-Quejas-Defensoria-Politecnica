package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejosoDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String unidadAcademica;
    private String tipoUsuario;
}