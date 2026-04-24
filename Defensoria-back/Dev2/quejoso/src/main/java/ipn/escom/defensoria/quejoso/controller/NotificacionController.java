package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.NotificacionDTO;
import ipn.escom.defensoria.quejoso.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/quejoso/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<NotificacionDTO> listar() {
        Long usuarioId = 1L; // Hardcoded hasta tener JWT
        return notificacionService.obtenerPorUsuario(usuarioId);
    }

    @PutMapping("/{id}/leer")
    public void leer(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
    }
}