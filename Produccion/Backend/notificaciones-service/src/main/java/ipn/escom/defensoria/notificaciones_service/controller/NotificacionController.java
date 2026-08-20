package ipn.escom.defensoria.notificaciones_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.notificaciones_service.entity.Notificacion;
import ipn.escom.defensoria.notificaciones_service.model.RegistrarNotificacionRequest;
import ipn.escom.defensoria.notificaciones_service.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Centro de notificaciones persistido por usuario")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // Público (ver WebConfig): lo llaman otros microservicios (auth-service, queja-service,
    // revision-service) para dejar un aviso -- no manda correo, solo persiste.
    @PostMapping("/registrar")
    @Operation(summary = "Registra una notificación persistida para un usuario (llamada interna entre microservicios)")
    public ResponseEntity<Notificacion> registrar(@RequestBody RegistrarNotificacionRequest datos) {
        return ResponseEntity.ok(notificacionService.registrar(datos));
    }

    // Protegido: el correo sale del JWT verificado (auth-service para quejosos).
    @GetMapping("/mias")
    @Operation(summary = "Lista las notificaciones del usuario autenticado (requiere JWT)")
    public ResponseEntity<List<Notificacion>> listarMias() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(notificacionService.listarMias(correo));
    }

    @GetMapping("/mias/no-leidas")
    @Operation(summary = "Cuenta las notificaciones no leídas del usuario autenticado (requiere JWT)")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(Map.of("noLeidas", notificacionService.contarNoLeidas(correo)));
    }

    @PutMapping("/{id}/leida")
    @Operation(summary = "Marca una notificación propia como leída (requiere JWT)")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable Long id) {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(notificacionService.marcarLeida(id, correo));
    }
}
