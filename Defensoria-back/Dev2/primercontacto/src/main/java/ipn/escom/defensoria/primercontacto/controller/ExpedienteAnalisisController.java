package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.service.ExpedienteAnalisisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/expedientes")
public class ExpedienteAnalisisController {

    private final ExpedienteAnalisisService expedienteAnalisisService;

    public ExpedienteAnalisisController(ExpedienteAnalisisService expedienteAnalisisService) {
        this.expedienteAnalisisService = expedienteAnalisisService;
    }

    @GetMapping("/{quejaId}")
    public ExpedienteAnalisisDTO obtenerExpediente(
            @PathVariable Long quejaId,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return expedienteAnalisisService.obtenerExpediente(quejaId, token);
    }

    @GetMapping("/folio/{folio}")
    public ExpedienteAnalisisDTO obtenerPorFolio(
            @PathVariable String folio,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return expedienteAnalisisService.obtenerPorFolio(folio, token);
    }
}