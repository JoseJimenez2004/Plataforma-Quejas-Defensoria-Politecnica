package ipn.escom.defensoria.chatbot_service.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ipn.escom.defensoria.chatbot_service.model.CategoriaChatbotModel;
import ipn.escom.defensoria.chatbot_service.service.PreguntaChatbotService;

/**
 * Endpoint público (ver WebConfig) que alimenta el widget de mini-chat/tutorial del portal
 * del quejoso (icono tipo mascota en el home y en "Presentar una queja"). No requiere sesión,
 * igual que el catálogo de dependencias.
 */
@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot", description = "Preguntas frecuentes preseleccionadas sobre la Defensoría y el proceso de queja")
public class ChatbotController {

    private final PreguntaChatbotService preguntaChatbotService;

    public ChatbotController(PreguntaChatbotService preguntaChatbotService) {
        this.preguntaChatbotService = preguntaChatbotService;
    }

    @GetMapping("/menu")
    @Operation(summary = "Menú completo del mini-chat: categorías con sus preguntas y respuestas, ya ordenadas")
    public ResponseEntity<List<CategoriaChatbotModel>> obtenerMenu() {
        return ResponseEntity.ok(preguntaChatbotService.obtenerMenuPublico());
    }
}
