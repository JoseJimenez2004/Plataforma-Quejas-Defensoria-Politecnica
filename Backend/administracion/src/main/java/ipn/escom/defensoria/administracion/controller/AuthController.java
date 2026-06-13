package ipn.escom.defensoria.administracion.controller;

import ipn.escom.defensoria.administracion.dto.AuthResponseDTO;
import ipn.escom.defensoria.administracion.dto.LoginRequestDTO;
import ipn.escom.defensoria.administracion.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AdminAuthService authService;

    public AuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        var response = authService.autenticar(request);
        return ResponseEntity.ok(response);
    }
}