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

    public RemisionExternaController(
            RemisionExternaService remisionExternaService
    ) {
        this.remisionExternaService =
                remisionExternaService;
    }

    @PostMapping
    public RemisionDTO crearRemision(
            @Valid @RequestBody CrearRemisionDTO dto
    ) {
        return remisionExternaService
                .crearRemision(dto);
    }

    @GetMapping("/expediente/{expedienteId}")
    public RemisionDTO obtenerPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return remisionExternaService
                .obtenerPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public RemisionDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return remisionExternaService
                .obtenerPorFolio(folio);
    }

    @PutMapping("/folio/{folio}/enviar")
    public RemisionDTO enviarRemision(
            @PathVariable String folio
    ) {
        return remisionExternaService
                .enviarRemision(folio);
    }
}