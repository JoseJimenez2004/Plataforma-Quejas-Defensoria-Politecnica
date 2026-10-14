package ipn.escom.defensoria.notificaciones_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
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
import ipn.escom.defensoria.notificaciones_service.model.EnviarCorreoRequest;
import ipn.escom.defensoria.notificaciones_service.model.RegistrarNotificacionRequest;
import ipn.escom.defensoria.notificaciones_service.service.CorreoService;
import ipn.escom.defensoria.notificaciones_service.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Centro de notificaciones persistido por usuario")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final CorreoService correoService;

    public NotificacionController(NotificacionService notificacionService, CorreoService correoService) {
        this.notificacionService = notificacionService;
        this.correoService = correoService;
    }

    // Público (ver WebConfig, ya tenía el permitAll): correo saliente puro, sin persistir.
    // Lo llaman revision-service (rechazo con observaciones) y queja-service (aviso al tutor).
    // Solo es alcanzable desde la VPS frontend y la red interna (firewall-backend.sh).
    @PostMapping("/enviar")
    @Operation(summary = "Envía un correo (llamada interna entre microservicios)")
    public ResponseEntity<Map<String, String>> enviar(@RequestBody EnviarCorreoRequest datos) {
        try {
            correoService.enviar(datos);
            return ResponseEntity.ok(Map.of("mensaje", "Correo enviado."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
        } catch (MailException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("mensaje", "No se pudo entregar el correo: " + ex.getMessage()));
        }
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
