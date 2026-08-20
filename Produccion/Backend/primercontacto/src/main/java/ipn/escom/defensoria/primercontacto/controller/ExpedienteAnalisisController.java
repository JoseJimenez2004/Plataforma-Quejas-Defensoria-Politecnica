package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.service.ExpedienteAnalisisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/expedientes")
public class ExpedienteAnalisisController {

    private final ExpedienteAnalisisService expedienteAnalisisService;

    public ExpedienteAnalisisController(
            ExpedienteAnalisisService expedienteAnalisisService
    ) {
        this.expedienteAnalisisService =
                expedienteAnalisisService;
    }

    /*
     * Consulta por ID interno de Primer Contacto.
     *
     * Ejemplo:
     * GET /api/primer-contacto/expedientes/1
     */
    @GetMapping("/{expedienteId}")
    public ExpedienteAnalisisDTO obtenerExpediente(
            @PathVariable Long expedienteId
    ) {
        return expedienteAnalisisService
                .obtenerExpediente(expedienteId);
    }

    /*
     * Consulta por folio propio de Primer Contacto.
     *
     * Ejemplo:
     * GET /api/primer-contacto/expedientes/folio/PC-A1B2C3D4
     */
    @GetMapping("/folio/{folio}")
    public ExpedienteAnalisisDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return expedienteAnalisisService
                .obtenerPorFolio(folio);
    }
}