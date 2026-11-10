package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.ResumenModeloService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 * Resumen de una queja con el modelo (resumen-service). Se guarda por folio la primera vez
 * que se genera y después siempre se devuelve el guardado.
 *
 * GET  /api/primer-contacto/resumen?folio=FOL-...
 *   200 -> ResumenQuejaDTO guardado
 *   204 -> todavía nadie lo ha generado
 *
 * POST /api/primer-contacto/resumen  { "folio": "FOL-...", "texto": "narrativa de la queja" }
 *   200 -> ResumenQuejaDTO (el guardado si ya existía; si no, lo genera y lo guarda)
 *   503 -> el modelo no está disponible o tardó demasiado (la pantalla usa el simulado)
 */
@RestController
@RequestMapping("/api/primer-contacto/resumen")
public class ResumenController {

    private final ResumenModeloService resumenService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public ResumenController(ResumenModeloService resumenService,
                             AnalistaAutenticadoService analistaAutenticadoService) {
        this.resumenService = resumenService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @GetMapping
    public ResponseEntity<?> guardado(@RequestParam String folio) {
        return resumenService.guardado(folio)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<?> resumir(@RequestBody Map<String, String> cuerpo, Authentication authentication) {
        String texto = cuerpo != null ? cuerpo.get("texto") : null;
        String folio = cuerpo != null ? cuerpo.get("folio") : null;
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La queja no tiene narrativa para resumir."));
        }
        PersonalAdministrativo analista = analistaAutenticadoService.obtenerAnalista(authentication);
        return resumenService.resumir(folio, texto, analista)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("mensaje", "El modelo de resumen no está disponible en este momento.")));
    }
}
