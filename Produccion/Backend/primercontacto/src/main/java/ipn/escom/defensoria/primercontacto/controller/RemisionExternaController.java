package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.service.RemisionExternaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;

@RestController
@RequestMapping("/api/primer-contacto/remisiones")
public class RemisionExternaController {

    private final RemisionExternaService remisionExternaService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public RemisionExternaController(
            RemisionExternaService remisionExternaService, AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.remisionExternaService =
                remisionExternaService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping
    public ResponseEntity<RemisionDTO> crearRemision(
            @Valid @RequestBody CrearRemisionDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                remisionExternaService.crearRemision(
                        dto,
                        analista
                )
        );
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