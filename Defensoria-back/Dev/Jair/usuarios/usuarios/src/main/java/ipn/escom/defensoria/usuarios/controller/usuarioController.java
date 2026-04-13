package ipn.escom.defensoria.usuarios.controller;

import ipn.escom.defensoria.usuarios.dto.usuarioDTO;
import ipn.escom.defensoria.usuarios.dto.LoginDTO;
import ipn.escom.defensoria.usuarios.service.usuarioService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200") // Para que Angular pueda entrar
public class usuarioController {

    @Autowired
    private usuarioService service;

    @PostMapping("/registro")
    public ResponseEntity<String> registrar(@RequestBody usuarioDTO datos) {
        String resultado = service.registrarUsuario(datos);
        if(resultado.contains("Error")) {
            return ResponseEntity.badRequest().body(resultado);
        }
        return ResponseEntity.ok(resultado);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        // Llamamos al servicio que devolverá un Map o un DTO
        Map<String, Object> respuesta = service.login(loginDTO);

        if ("success".equals(respuesta.get("status"))) {
            return ResponseEntity.ok(respuesta);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }
    } 
   
    @PostMapping("/recuperar")
    public ResponseEntity<String> solicitar(@RequestParam String correo) {
        return ResponseEntity.ok(service.solicitarRecuperacion(correo));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> reset(@RequestParam String token, @RequestParam String password) {
        return ResponseEntity.ok(service.cambiarPasswordConToken(token, password));
    }

    // Obtenemos todas las notificaciones de un usuario
    @GetMapping("/{correo}/notificaciones")
    public ResponseEntity<?> obtenerNotificaciones(@PathVariable String correo) {
        try {
            return ResponseEntity.ok(service.obtenerNotificacionesUsuario(correo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
    }

    // Marcamos una notificación como leída (para quitar el punto rojo)
    @PatchMapping("/notificaciones/{id}/leer")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        service.marcarNotificacionComoLeida(id);
        return ResponseEntity.ok().build();
    }
    // En usuarioController.java
    @PostMapping("/notificar")
    public ResponseEntity<String> crearNotificacion(
            @RequestParam String correo, 
            @RequestParam String titulo, 
            @RequestParam String mensaje) {
        try {
            service.crearNotificacion(correo, titulo, mensaje);
            return ResponseEntity.ok("Notificación enviada");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/perfil/{correo}")
    public ResponseEntity<?> obtenerDatosPrecarga(@PathVariable String correo) {
        try {
            return ResponseEntity.ok(service.obtenerPerfilParaPrecarga(correo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
    }
    
    @PostMapping("/vincular")
    public ResponseEntity<String> vincularFolio(@RequestParam String correo, @RequestParam String folio) {
        service.agregarNuevoFolio(correo, folio); // Este método sí lo tienes en el service
        return ResponseEntity.ok("Folio vinculado exitosamente");
    }
}
