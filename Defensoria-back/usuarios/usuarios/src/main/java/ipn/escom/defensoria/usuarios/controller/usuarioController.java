package ipn.escom.defensoria.usuarios.controller;

import ipn.escom.defensoria.usuarios.dto.usuarioDTO;
import ipn.escom.defensoria.usuarios.service.usuarioService;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @PostMapping("/recuperar")
    public ResponseEntity<String> solicitar(@RequestParam String correo) {
        return ResponseEntity.ok(service.solicitarRecuperacion(correo));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> reset(@RequestParam String token, @RequestParam String password) {
        return ResponseEntity.ok(service.cambiarPasswordConToken(token, password));
    }
}
