package ipn.escom.defensoria.quejoso.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioPerfilDTO {
    private String nombre;
    private String correoInstitucional;
    private String boleta;
    private String unidadAcademica;

    // Campos editables (MQ-20)
    private String correoPersonal;
    private String telefonoCelular;
    private String nombreTutor;
    private String parentescoTutor;
    private String telefonoTutor;
}