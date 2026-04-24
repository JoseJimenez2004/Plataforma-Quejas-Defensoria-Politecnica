package ipn.escom.defensoria.quejoso.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRespuestaDTO {
    private String token;
    private UsuarioPerfilDTO perfil;
}