package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CancelacionCitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CitaPublicaDTO;
import ipn.escom.defensoria.primercontacto.service.CitaPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Respuesta del quejoso a su cita desde el ENLACE que le llega por correo y en su centro de
 * notificaciones (página /cita/responder?token=... del portal). No pide sesión: el token
 * aleatorio de la cita es la credencial, igual que un enlace de activación de cuenta.
 *
 *   GET /api/primer-contacto/publico/citas/{token}
 *   PUT /api/primer-contacto/publico/citas/{token}/confirmar
 *   PUT /api/primer-contacto/publico/citas/{token}/cancelar      { "motivo": "..." }
 *
 * Confirmar y cancelar son PUT (no GET) a propósito: los antivirus de correo abren los
 * enlaces para revisarlos, y un GET que confirmara lo haría sin que el quejoso decidiera.
 */
@RestController
@RequestMapping("/api/primer-contacto/publico/citas")
public class CitaPublicaController {

    private final CitaPrimerContactoService citaService;

    public CitaPublicaController(CitaPrimerContactoService citaService) {
        this.citaService = citaService;
    }

    @GetMapping("/{token}")
    public CitaPublicaDTO consultar(@PathVariable String token) {
        return citaService.citaPorToken(token);
    }

    @PutMapping("/{token}/confirmar")
    public CitaPublicaDTO confirmar(@PathVariable String token) {
        return citaService.confirmarPorToken(token);
    }

    @PutMapping("/{token}/cancelar")
    public CitaPublicaDTO cancelar(
            @PathVariable String token,
            @Valid @RequestBody CancelacionCitaDTO dto
    ) {
        return citaService.cancelarPorToken(token, dto.getMotivo());
    }
}
