package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.service.RemisionExternaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/remisiones")
public class RemisionExternaController {

    private final RemisionExternaService remisionExternaService;

    public RemisionExternaController(RemisionExternaService remisionExternaService) {
        this.remisionExternaService = remisionExternaService;
    }

    @PostMapping
    public RemisionDTO crearRemision(
            @Valid @RequestBody CrearRemisionDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return remisionExternaService.crearRemision(dto, token);
    }

    @GetMapping("/queja/{quejaId}")
    public RemisionDTO obtenerPorQueja(
            @PathVariable Long quejaId
    ) {
        return remisionExternaService.obtenerPorQueja(quejaId);
    }

    @GetMapping("/folio/{folio}")
    public RemisionDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return remisionExternaService.obtenerPorFolio(folio);
    }

    @PutMapping("/queja/{quejaId}/enviar")
    public RemisionDTO enviarRemision(
            @PathVariable Long quejaId,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return remisionExternaService.enviarRemision(quejaId, token);
    }
}