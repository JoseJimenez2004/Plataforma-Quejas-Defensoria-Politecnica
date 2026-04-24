package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.QuejaFiltroDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaSeguimientoDTO;
import ipn.escom.defensoria.quejoso.dto.TramitesResumenDTO;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.service.QuejaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
        // Extraemos al usuario real desde la "caja fuerte" de Spring
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ResponseEntity.ok(quejaService.obtenerResumenDashboard(usuarioAutenticado.getId()));
    }

    // MQ-15: Listado completo (Historial)
    @GetMapping("/historial")
    public ResponseEntity<List<QuejaSeguimientoDTO>> getHistorial(QuejaFiltroDTO filtro) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ResponseEntity.ok(quejaService.obtenerHistorialFiltrado(usuarioAutenticado.getId(), filtro));
    }

    // MQ-12: Confirmar eliminación (Cancelación Lógica)
    @DeleteMapping("/{folio}")
    public ResponseEntity<String> eliminar(@PathVariable String folio) {
        try {
            Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            quejaService.eliminarQueja(folio, usuarioAutenticado.getId());
            return ResponseEntity.ok("Queja cancelada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}