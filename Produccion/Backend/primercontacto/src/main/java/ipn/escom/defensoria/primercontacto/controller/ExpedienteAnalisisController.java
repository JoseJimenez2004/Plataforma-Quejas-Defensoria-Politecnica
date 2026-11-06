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

    private static final java.util.Set<String> PRIORIDADES = java.util.Set.of("BAJA", "MEDIA", "ALTA", "URGENTE");

    /*
     * PC-1: el analista asigna la prioridad del expediente.
     *
     * PATCH /api/primer-contacto/expedientes/folio/PC-A1B2C3D4/prioridad  { "prioridad": "ALTA" }
     * Solo BAJA, MEDIA, ALTA o URGENTE; cualquier otro valor responde 400.
     * Rol ANALISTA_PRIMER_CONTACTO (lo exige SecurityConfig para /api/primer-contacto/**).
     */
    @PatchMapping("/folio/{folio}/prioridad")
    public org.springframework.http.ResponseEntity<?> cambiarPrioridad(
            @PathVariable String folio,
            @RequestBody java.util.Map<String, String> cuerpo
    ) {
        String valor = cuerpo == null || cuerpo.get("prioridad") == null
                ? "" : cuerpo.get("prioridad").trim().toUpperCase(java.util.Locale.ROOT);
        if (!PRIORIDADES.contains(valor)) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Prioridad inválida. Usa BAJA, MEDIA, ALTA o URGENTE."));
        }
        return org.springframework.http.ResponseEntity.ok(expedienteAnalisisService.cambiarPrioridad(folio, valor));
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
