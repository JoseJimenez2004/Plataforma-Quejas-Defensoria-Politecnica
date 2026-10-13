package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoPublicoDTO;
import ipn.escom.defensoria.subdefensoria.dto.RespuestaAcuerdoDTO;
import ipn.escom.defensoria.subdefensoria.service.AcuerdoConclusionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * CU-QJ-14: el quejoso acepta o rechaza el acuerdo de conciliación desde el ENLACE que le
 * llega por correo y en su centro de notificaciones (página /acuerdo/responder?token=... del
 * portal). No pide sesión: el token aleatorio del acuerdo es la credencial.
 *
 *   GET /api/subdefensoria/publico/acuerdos/{token}
 *   PUT /api/subdefensoria/publico/acuerdos/{token}/aceptar     { "comentario": "..." } (opcional)
 *   PUT /api/subdefensoria/publico/acuerdos/{token}/rechazar    { "comentario": "..." } (obligatorio)
 *
 * Aceptar y rechazar son PUT (no GET): los antivirus de correo abren los enlaces para
 * revisarlos, y un GET que respondiera lo haría sin que el quejoso decidiera.
 */
@RestController
@RequestMapping("/api/subdefensoria/publico/acuerdos")
public class AcuerdoPublicoController {

    private final AcuerdoConclusionService acuerdoService;

    public AcuerdoPublicoController(AcuerdoConclusionService acuerdoService) {
        this.acuerdoService = acuerdoService;
    }

    @GetMapping("/{token}")
    public AcuerdoPublicoDTO consultar(@PathVariable String token) {
        return acuerdoService.acuerdoPorToken(token);
    }

    @PutMapping("/{token}/aceptar")
    public AcuerdoPublicoDTO aceptar(@PathVariable String token,
                                     @Valid @RequestBody(required = false) RespuestaAcuerdoDTO dto) {
        return acuerdoService.aceptarPorToken(token, dto != null ? dto.getComentario() : null);
    }

    @PutMapping("/{token}/rechazar")
    public AcuerdoPublicoDTO rechazar(@PathVariable String token,
                                      @Valid @RequestBody RespuestaAcuerdoDTO dto) {
        return acuerdoService.rechazarPorToken(token, dto.getComentario());
    }
}
