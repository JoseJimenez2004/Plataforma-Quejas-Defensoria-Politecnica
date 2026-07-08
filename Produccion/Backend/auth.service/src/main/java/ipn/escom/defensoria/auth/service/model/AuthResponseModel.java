package ipn.escom.defensoria.auth.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseModel {
    private String token;
    private String nombre;
}