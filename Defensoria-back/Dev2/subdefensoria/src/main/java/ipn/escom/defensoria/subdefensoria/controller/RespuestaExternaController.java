package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.RegistrarRespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.dto.RespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.service.RespuestaExternaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/respuestas-externas")
public class RespuestaExternaController {

    private final RespuestaExternaService respuestaExternaService;

    public RespuestaExternaController(RespuestaExternaService respuestaExternaService) {
        this.respuestaExternaService = respuestaExternaService;
    }

    /** Boton "Confirmar Registro y Habilitar Dictamen" en P15. */
    @PostMapping
    public RespuestaExternaDTO registrarRespuesta(@Valid @RequestBody RegistrarRespuestaExternaDTO dto) {
        return respuestaExternaService.registrarRespuesta(dto);
    }
}
