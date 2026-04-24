package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.NotificacionDTO;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/quejoso/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<NotificacionDTO> listar() {
        // Obtenemos al usuario real desde la "caja fuerte" de Spring
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Usamos su ID real
        return notificacionService.obtenerPorUsuario(usuarioAutenticado.getId());
    }

    @PutMapping("/{id}/leer")
    public void leer(@PathVariable Long id) {
        // Aquí podrías agregar una validación en el Service para asegurar
        // que la notificación que se marca como leída realmente le pertenece al usuario logueado.
        notificacionService.marcarComoLeida(id);
    }
}