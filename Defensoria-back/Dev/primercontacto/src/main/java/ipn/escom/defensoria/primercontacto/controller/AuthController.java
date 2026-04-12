package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.entity.Usuario;
import ipn.escom.defensoria.primercontacto.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginRequest) {
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(loginRequest.getEmail());
        
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(loginRequest.getPassword())) {
            // En un sistema real, aquí generarías un JWT token
            return ResponseEntity.ok(userOpt.get());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
    }
}