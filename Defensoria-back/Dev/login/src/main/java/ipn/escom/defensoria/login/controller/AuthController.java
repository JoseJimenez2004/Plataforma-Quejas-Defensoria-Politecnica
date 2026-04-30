package ipn.escom.defensoria.login.controller;

import ipn.escom.defensoria.login.dto.RegistroRequest;
import ipn.escom.defensoria.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(@RequestBody RegistroRequest request) {
        String mensaje = authService.registrarUsuario(request);
        return ResponseEntity.ok(mensaje);
    }
}