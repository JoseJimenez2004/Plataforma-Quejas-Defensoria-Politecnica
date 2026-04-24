package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.config.JwtUtil;
import ipn.escom.defensoria.quejoso.dto.ActivacionCuentaDTO;
import ipn.escom.defensoria.quejoso.dto.AuthRespuestaDTO;
import ipn.escom.defensoria.quejoso.dto.LoginDTO;
import ipn.escom.defensoria.quejoso.dto.UsuarioPerfilDTO;
import ipn.escom.defensoria.quejoso.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quejoso/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Endpoint para MQ-05 y MQ-09
     * Activa la cuenta mediante el folio de la queja
     */
    @PostMapping("/activar-cuenta")
    public ResponseEntity<String> activar(@RequestBody ActivacionCuentaDTO dto) {
        try {
            usuarioService.activarCuenta(dto);
            return ResponseEntity.ok("Cuenta activada con éxito. Ya puedes iniciar sesión.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        try {
            // 1. Validamos credenciales
            UsuarioPerfilDTO perfil = usuarioService.login(dto);

            // 2. Si son correctas, generamos su "pase" (Token)
            String token = jwtUtil.generarToken(dto.getCorreo());

            // 3. Entregamos el pase y los datos
            return ResponseEntity.ok(new AuthRespuestaDTO(token, perfil));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }


}