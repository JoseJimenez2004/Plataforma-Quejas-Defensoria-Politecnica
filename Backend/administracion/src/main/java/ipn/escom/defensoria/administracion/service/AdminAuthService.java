package ipn.escom.defensoria.administracion.service;

import ipn.escom.defensoria.administracion.dto.AuthResponseDTO;
import ipn.escom.defensoria.administracion.dto.LoginRequestDTO;
import ipn.escom.defensoria.administracion.entity.UsuarioAdmin;
import ipn.escom.defensoria.administracion.repository.UsuarioAdminRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioAdminRepository usuarioRepository;

    public AdminAuthService(AuthenticationManager authenticationManager, UsuarioAdminRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
    }

    public AuthResponseDTO autenticar(LoginRequestDTO request) {
        try {
            // 1. Spring Security valida el correo y la contraseña contra la BD real
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correo(), request.password())
            );
        } catch (Exception e) {
            // Si la contraseña no es "123456" (la de la BD), cae aquí y lanza el 401
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // 2. Si las credenciales son válidas, buscamos al usuario para obtener su nombre real
        UsuarioAdmin usuario = usuarioRepository.findByCorreoInstitucional(request.correo())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado en BD"));

        // Nota: Como vi en tu BD que aún no tienes columna de roles, asignamos este temporalmente
        List<String> rolesTemporales = List.of("ROLE_ADMIN_TI"); 

        // Mantendremos tu token simulado intacto hasta que programes la generación real del JWT
        var tokenSimulado = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.simulacion.token";

        // Devolvemos el DTO usando el nombre completo que viene directamente de tu tabla en Neon
        return new AuthResponseDTO(tokenSimulado, usuario.getNombreCompleto(), rolesTemporales);
    }
}