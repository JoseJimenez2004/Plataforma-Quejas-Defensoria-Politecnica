package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.UsuarioPerfilDTO;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quejoso/perfil")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<UsuarioPerfilDTO> verPerfil() {
        // Obtenemos al dueño del Token actual
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ResponseEntity.ok(usuarioService.obtenerPerfil(usuarioAutenticado.getId()));
    }

    @PutMapping("/actualizar")
    public ResponseEntity<String> actualizar(@RequestBody UsuarioPerfilDTO dto) {
        // Evitamos que alguien actualice un perfil ajeno
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        usuarioService.actualizarPerfil(usuarioAutenticado.getId(), dto);
        return ResponseEntity.ok("Perfil actualizado correctamente");
    }
}