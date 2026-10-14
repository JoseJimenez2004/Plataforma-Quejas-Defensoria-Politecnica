package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearSolicitudDenunciadoDTO;
import ipn.escom.defensoria.primercontacto.dto.SolicitudDenunciadoDTO;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.SolicitudDenunciadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Solicitud al denunciado de su versión de los hechos (correo con el enlace al formulario).
 *
 * GET  /api/primer-contacto/expedientes/folio/PC-A1B2C3D4/denunciado/solicitudes
 * POST /api/primer-contacto/expedientes/folio/PC-A1B2C3D4/denunciado/solicitudes  { "correo": "..." }
 */
@RestController
@RequestMapping("/api/primer-contacto/expedientes/folio/{folio}/denunciado/solicitudes")
public class SolicitudDenunciadoController {

    private final SolicitudDenunciadoService solicitudService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public SolicitudDenunciadoController(
            SolicitudDenunciadoService solicitudService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.solicitudService = solicitudService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @GetMapping
    public List<SolicitudDenunciadoDTO> listar(@PathVariable String folio) {
        return solicitudService.listar(folio);
    }

    @PostMapping
    public ResponseEntity<SolicitudDenunciadoDTO> enviar(
            @PathVariable String folio,
            @Valid @RequestBody CrearSolicitudDenunciadoDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitudService.enviar(
                folio, dto.getCorreo(), analistaAutenticadoService.obtenerAnalista(authentication)));
    }
}
