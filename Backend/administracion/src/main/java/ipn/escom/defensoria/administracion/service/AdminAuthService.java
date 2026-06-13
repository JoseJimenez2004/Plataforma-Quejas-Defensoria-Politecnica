package ipn.escom.defensoria.administracion.service;

import ipn.escom.defensoria.administracion.dto.AuthResponseDTO;
import ipn.escom.defensoria.administracion.dto.LoginRequestDTO;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AdminAuthService {

    public AuthResponseDTO autenticar(LoginRequestDTO request) {
        
        if (!request.password().equals("Admin123!")) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        var rolesSimulados = switch (request.correo()) {
            case "ti@ipn.mx" -> List.of("ROLE_ADMIN_TI");
            case "recepcion@ipn.mx" -> List.of("ROLE_RECEPCION");
            case "subdefensor@ipn.mx" -> List.of("ROLE_SUBDEFENSOR");
            case "defensoria@ipn.mx" -> List.of("ROLE_DEFENSORIA");
            case "contacto@ipn.mx" -> List.of("ROLE_PRIMER_CONTACTO");
            default -> throw new IllegalArgumentException("Usuario no reconocido en el panel administrativo");
        };

        var tokenSimulado = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.simulacion.token";

        return new AuthResponseDTO(tokenSimulado, "Empleado Defensoría", rolesSimulados);
    }
}