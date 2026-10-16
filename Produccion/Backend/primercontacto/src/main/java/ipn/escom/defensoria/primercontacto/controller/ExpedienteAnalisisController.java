package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.ExpedienteAnalisisService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/expedientes")
public class ExpedienteAnalisisController {

    private final ExpedienteAnalisisService expedienteAnalisisService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public ExpedienteAnalisisController(
            ExpedienteAnalisisService expedienteAnalisisService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.expedienteAnalisisService =
                expedienteAnalisisService;
        this.analistaAutenticadoService = analistaAutenticadoService;
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

    /*
     * CU-PC-03: el analista abre el expediente para trabajarlo.
     * TURNADA -> EN_ANALISIS; en otro estado solo devuelve el expediente.
     *
     * POST /api/primer-contacto/expedientes/folio/PC-A1B2C3D4/iniciar-analisis
     */
    @PostMapping("/folio/{folio}/iniciar-analisis")
    public ExpedienteAnalisisDTO iniciarAnalisis(
            @PathVariable String folio,
            Authentication authentication
    ) {
        return expedienteAnalisisService.iniciarAnalisis(
                folio,
                analistaAutenticadoService.obtenerAnalista(authentication)
        );
    }
}
