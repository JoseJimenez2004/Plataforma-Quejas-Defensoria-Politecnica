package ipn.escom.defensoria.auth.service.controller;

import ipn.escom.defensoria.auth.service.client.NotificacionesClient;
import ipn.escom.defensoria.auth.service.config.JwtUtil;
import ipn.escom.defensoria.auth.service.entity.Usuario;
import ipn.escom.defensoria.auth.service.model.LoginModel;
import ipn.escom.defensoria.auth.service.model.AuthResponseModel;
import ipn.escom.defensoria.auth.service.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ipn.escom.defensoria.auth.service.model.ResetPasswordModel;
import org.springframework.web.bind.annotation.RequestParam;
import ipn.escom.defensoria.auth.service.model.ActivacionCuentaModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String TIPO_NOTIFICACION_LOGIN = "LOGIN";

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final NotificacionesClient notificacionesClient;

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil, NotificacionesClient notificacionesClient) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
        this.notificacionesClient = notificacionesClient;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginModel model) {
        try {
            // 1. Validamos credenciales en la base de datos
            Usuario usuario = usuarioService.validarLogin(model);

            // 2. Generamos el JWT usando el correo institucional
            String token = jwtUtil.generarToken(usuario.getCorreoInstitucional());

            // 3. Dejamos un aviso persistido en el centro de notificaciones del quejoso
            // ("inicios de sesión" que pidió el usuario) -- si falla, no debe tumbar el
            // login, solo queda en el log para diagnosticar (mismo patrón de resiliencia que
            // ya usa revision-service al mandar el correo de rechazo).
            try {
                String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                notificacionesClient.registrar(Map.of(
                        "correoDestino", usuario.getCorreoInstitucional(),
                        "tipo", TIPO_NOTIFICACION_LOGIN,
                        "titulo", "Inicio de sesión",
                        "mensaje", "Se inició sesión en tu cuenta el " + fecha + "."));
            } catch (Exception ex) {
                log.error("No se pudo registrar la notificación de inicio de sesión para {}: {}",
                        usuario.getCorreoInstitucional(), ex.getMessage());
            }

            // 4. Entregamos el token al cliente
            return ResponseEntity.ok(new AuthResponseModel(token, usuario.getNombre()));

        } catch (RuntimeException e) {
            // Si el servicio lanza la excepción de credenciales incorrectas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Estos 3 endpoints regresaban texto plano (ResponseEntity<String>), pero los errores
    // pasan por GlobalExceptionHandler y responden JSON ({mensaje,...}). El frontend pedía
    // la respuesta con responseType:'text', así que en caso de error "err.error" llegaba
    // como string crudo en vez de objeto — el mensaje real del backend (ej. "Correo no
    // registrado") nunca se mostraba, solo un texto genérico de respaldo. Unificamos todo a
    // JSON para que éxito y error se lean igual en el frontend.
    @PostMapping("/solicitar-codigo")
    public ResponseEntity<Map<String, String>> solicitarCodigo(@RequestParam String correo) {
        usuarioService.generarCodigoRecuperacion(correo);
        return ResponseEntity.ok(Map.of("mensaje", "Código de verificación enviado a tu correo."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordModel model) {
        usuarioService.validarCodigoYCambiarPassword(
                model.getCorreo(),
                model.getCodigo(),
                model.getNuevaPassword()
        );
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada con éxito. Ya puedes iniciar sesión."));
    }

    @PostMapping("/activar-cuenta")
    public ResponseEntity<Map<String, String>> activar(@RequestBody ActivacionCuentaModel model) {
        usuarioService.activarCuenta(model);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta activada con éxito. Ya puedes iniciar sesión."));
    }
}