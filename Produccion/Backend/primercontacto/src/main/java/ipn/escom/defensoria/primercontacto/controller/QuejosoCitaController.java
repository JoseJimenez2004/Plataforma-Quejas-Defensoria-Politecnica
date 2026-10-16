package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CancelacionCitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.service.CitaPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Citas vistas desde el PORTAL DEL QUEJOSO: consulta y respuesta (confirmar o cancelar con
 * motivo) dentro del plazo. La pantalla del portal la construye el equipo del quejoso; este
 * es el contrato que consume.
 *
 * El token del quejoso no lleva rol (ver auth-service/JwtUtil): solo se exige que esté
 * autenticado, y el correo del token debe ser el del quejoso del expediente.
 *
 *   GET /api/primer-contacto/quejoso/citas/mias
 *   PUT /api/primer-contacto/quejoso/citas/{id}/confirmar
 *   PUT /api/primer-contacto/quejoso/citas/{id}/cancelar      { "motivo": "..." }
 */
@RestController
@RequestMapping("/api/primer-contacto/quejoso/citas")
public class QuejosoCitaController {

    private final CitaPrimerContactoService citaService;

    public QuejosoCitaController(CitaPrimerContactoService citaService) {
        this.citaService = citaService;
    }

    @GetMapping("/mias")
    public List<CitaDTO> misCitas(Authentication authentication) {
        return citaService.citasDelQuejoso(correo(authentication));
    }

    @PutMapping("/{id}/confirmar")
    public CitaDTO confirmar(@PathVariable Long id, Authentication authentication) {
        return citaService.confirmarPorQuejoso(id, correo(authentication));
    }

    @PutMapping("/{id}/cancelar")
    public CitaDTO cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelacionCitaDTO dto,
            Authentication authentication
    ) {
        return citaService.cancelarPorQuejoso(id, dto.getMotivo(), correo(authentication));
    }

    private String correo(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return authentication.getName();
    }
}
