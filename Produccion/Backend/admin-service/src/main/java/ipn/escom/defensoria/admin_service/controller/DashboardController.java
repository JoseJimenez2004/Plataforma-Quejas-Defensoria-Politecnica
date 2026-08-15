package ipn.escom.defensoria.admin_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.model.DashboardResumenModel;
import ipn.escom.defensoria.admin_service.service.DashboardService;

/** "Configuración General" del mockup -- el dashboard principal del panel. */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Dashboard", description = "Resumen general del sistema")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/resumen")
    @Operation(summary = "Resumen para las 4 tarjetas de Configuración General")
    public ResponseEntity<DashboardResumenModel> resumen() {
        return ResponseEntity.ok(dashboardService.obtenerResumen());
    }
}
