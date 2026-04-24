package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.UsuarioPerfilDTO;
import ipn.escom.defensoria.quejoso.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quejoso/perfil")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<UsuarioPerfilDTO> verPerfil() {
        Long usuarioId = 1L; // Temporal
        return ResponseEntity.ok(usuarioService.obtenerPerfil(usuarioId));
    }

    @PutMapping("/actualizar")
    public ResponseEntity<String> actualizar(@RequestBody UsuarioPerfilDTO dto) {
        Long usuarioId = 1L; // Temporal
        usuarioService.actualizarPerfil(usuarioId, dto);
        return ResponseEntity.ok("Perfil actualizado correctamente");
    }
}