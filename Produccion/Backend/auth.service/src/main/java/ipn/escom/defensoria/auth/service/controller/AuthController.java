package ipn.escom.defensoria.auth.service.controller;

import ipn.escom.defensoria.auth.service.config.JwtUtil;
import ipn.escom.defensoria.auth.service.entity.Usuario;
import ipn.escom.defensoria.auth.service.model.LoginModel;
import ipn.escom.defensoria.auth.service.model.AuthResponseModel;
import ipn.escom.defensoria.auth.service.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ipn.escom.defensoria.auth.service.model.ResetPasswordModel;
import org.springframework.web.bind.annotation.RequestParam;
import ipn.escom.defensoria.auth.service.model.ActivacionCuentaModel;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginModel model) {
        try {
            // 1. Validamos credenciales en la base de datos
            Usuario usuario = usuarioService.validarLogin(model);
            
            // 2. Generamos el JWT usando el correo institucional
            String token = jwtUtil.generarToken(usuario.getCorreoInstitucional());
            
            // 3. Entregamos el token al cliente
            return ResponseEntity.ok(new AuthResponseModel(token, usuario.getNombre()));
            
        } catch (RuntimeException e) {
            // Si el servicio lanza la excepción de credenciales incorrectas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/solicitar-codigo")
    public ResponseEntity<String> solicitarCodigo(@RequestParam String correo) {
        usuarioService.generarCodigoRecuperacion(correo);
        return ResponseEntity.ok("Código de verificación enviado a tu correo.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordModel model) {
        usuarioService.validarCodigoYCambiarPassword(
                model.getCorreo(),
                model.getCodigo(),
                model.getNuevaPassword()
        );
        return ResponseEntity.ok("Contraseña actualizada con éxito. Ya puedes iniciar sesión.");
    }

    @PostMapping("/activar-cuenta")
    public ResponseEntity<String> activar(@RequestBody ActivacionCuentaModel model) {
        usuarioService.activarCuenta(model);
        return ResponseEntity.ok("Cuenta activada con éxito. Ya puedes iniciar sesión.");
    }
}