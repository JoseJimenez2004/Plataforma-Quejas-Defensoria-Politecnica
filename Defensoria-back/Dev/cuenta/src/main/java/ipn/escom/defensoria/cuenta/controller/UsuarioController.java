package ipn.escom.defensoria.cuenta.controller;

import ipn.escom.defensoria.cuenta.dto.LoginRequest;
import ipn.escom.defensoria.cuenta.dto.RegistroRequest;
import ipn.escom.defensoria.cuenta.dto.UsuarioResponse;
import ipn.escom.defensoria.cuenta.service.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") 
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    /**
     * Usa RegistroRequest en lugar de UsuarioEntity
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody RegistroRequest registroDTO) {
        try {
            UsuarioResponse respuesta = usuarioService.registrar(registroDTO);
            return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Usa LoginRequest para empaquetar correo y password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginDTO) {
        try {
            UsuarioResponse respuesta = usuarioService.login(loginDTO);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<?> recuperarPassword(@RequestBody Map<String, String> request) {
        try {
            String correo = request.get("correo");
            usuarioService.recuperarPassword(correo);
            
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Se ha enviado un correo con las instrucciones.");
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }
}