package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.QuejaFiltroDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaSeguimientoDTO;
import ipn.escom.defensoria.quejoso.dto.TramitesResumenDTO;
import ipn.escom.defensoria.quejoso.service.QuejaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quejoso/tramites")
public class TramitesController {

    @Autowired
    private QuejaService quejaService;

    // MQ-11: Dashboard principal
    @GetMapping("/resumen")
    public ResponseEntity<TramitesResumenDTO> getResumen() {
        Long usuarioId = 1L; // Temporalmente hardcoded hasta tener el SecurityContext
        return ResponseEntity.ok(quejaService.obtenerResumenDashboard(usuarioId));
    }

    // MQ-15: Listado completo (Historial)
    // En TramitesController.java
    @GetMapping("/historial")
    public ResponseEntity<List<QuejaSeguimientoDTO>> getHistorial(QuejaFiltroDTO filtro) {
        Long usuarioId = 1L; // Temporal
        return ResponseEntity.ok(quejaService.obtenerHistorialFiltrado(usuarioId, filtro));
    }
    // MQ-12: Confirmar eliminación
    @DeleteMapping("/{folio}")
    public ResponseEntity<String> eliminar(@PathVariable String folio) {
        try {
            quejaService.eliminarQueja(folio, 1L);
            return ResponseEntity.ok("Queja eliminada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}