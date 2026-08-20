package ipn.escom.defensoria.chatbot_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.chatbot_service.dto.PreguntaChatbotRequest;
import ipn.escom.defensoria.chatbot_service.entity.PreguntaChatbot;
import ipn.escom.defensoria.chatbot_service.service.PreguntaChatbotService;

/**
 * CRUD administrativo del contenido del chatbot -- a diferencia de ChatbotController
 * (público, solo lectura), esto exige un JWT con rol ADMIN_SISTEMAS emitido por
 * admin-service (mismo jwt.secret compartido entre microservicios). Permite editar/agregar
 * preguntas sin necesidad de redesplegar el servicio.
 */
@RestController
@RequestMapping("/api/chatbot/admin")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Chatbot (Admin)", description = "Alta, edición y baja de las preguntas del chatbot")
public class ChatbotAdminController {

    private final PreguntaChatbotService preguntaChatbotService;

    public ChatbotAdminController(PreguntaChatbotService preguntaChatbotService) {
        this.preguntaChatbotService = preguntaChatbotService;
    }

    @GetMapping("/preguntas")
    @Operation(summary = "Lista TODAS las preguntas (activas e inactivas) para el panel de administración")
    public ResponseEntity<List<PreguntaChatbot>> listarTodas() {
        return ResponseEntity.ok(preguntaChatbotService.listarTodas());
    }

    @PostMapping("/preguntas")
    @Operation(summary = "Crea una nueva pregunta")
    public ResponseEntity<PreguntaChatbot> crear(@RequestBody PreguntaChatbotRequest datos) {
        return ResponseEntity.ok(preguntaChatbotService.crear(datos));
    }

    @PutMapping("/preguntas/{id}")
    @Operation(summary = "Edita una pregunta existente")
    public ResponseEntity<PreguntaChatbot> editar(@PathVariable Long id, @RequestBody PreguntaChatbotRequest datos) {
        return ResponseEntity.ok(preguntaChatbotService.editar(id, datos));
    }

    @DeleteMapping("/preguntas/{id}")
    @Operation(summary = "Elimina una pregunta")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        preguntaChatbotService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
