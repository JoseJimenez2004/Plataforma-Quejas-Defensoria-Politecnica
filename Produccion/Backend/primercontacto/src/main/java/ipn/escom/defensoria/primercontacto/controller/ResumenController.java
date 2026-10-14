package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.service.ResumenModeloService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 * Borrador de resumen de una queja con el modelo (resumen-service).
 *
 * POST /api/primer-contacto/resumen  { "texto": "narrativa de la queja" }
 *   200 -> ResumenQuejaDTO
 *   503 -> el modelo no está disponible o tardó demasiado (la pantalla usa el simulado)
 */
@RestController
@RequestMapping("/api/primer-contacto/resumen")
public class ResumenController {

    private final ResumenModeloService resumenService;

    public ResumenController(ResumenModeloService resumenService) {
        this.resumenService = resumenService;
    }

    @PostMapping
    public ResponseEntity<?> resumir(@RequestBody Map<String, String> cuerpo) {
        String texto = cuerpo != null ? cuerpo.get("texto") : null;
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La queja no tiene narrativa para resumir."));
        }
        return resumenService.resumir(texto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("mensaje", "El modelo de resumen no está disponible en este momento.")));
    }
}
