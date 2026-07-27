package ipn.escom.defensoria.admin_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.entity.PlantillaDocumento;
import ipn.escom.defensoria.admin_service.model.PlantillaUpdateRequest;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.PlantillaService;
import jakarta.servlet.http.HttpServletRequest;

/** "Plantillas Oficiales" del mockup -- exclusivo de ADMIN_SISTEMAS. */
@RestController
@RequestMapping("/api/admin/plantillas")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Plantillas Oficiales", description = "Edición de plantillas de oficios y formatos legales")
public class PlantillaController {

    @Autowired
    private PlantillaService plantillaService;

    @Autowired
    private BitacoraService bitacoraService;

    @GetMapping
    @Operation(summary = "Lista todas las plantillas disponibles")
    public ResponseEntity<List<PlantillaDocumento>> listar() {
        return ResponseEntity.ok(plantillaService.listar());
    }

    @GetMapping("/placeholders")
    @Operation(summary = "Lista los placeholders disponibles y su significado")
    public ResponseEntity<Map<String, String>> placeholders() {
        return ResponseEntity.ok(plantillaService.placeholdersDisponibles());
    }

    @GetMapping("/{tipo}")
    @Operation(summary = "Obtiene una plantilla por su tipo")
    public ResponseEntity<PlantillaDocumento> obtener(@PathVariable String tipo) {
        return ResponseEntity.ok(plantillaService.obtener(tipo));
    }

    @GetMapping("/{tipo}/previsualizar")
    @Operation(summary = "Previsualiza la plantilla con datos de ejemplo")
    public ResponseEntity<String> previsualizar(@PathVariable String tipo) {
        return ResponseEntity.ok(plantillaService.previsualizar(tipo));
    }

    @PutMapping("/{tipo}")
    @Operation(summary = "Actualiza el contenido de una plantilla y la publica")
    public ResponseEntity<PlantillaDocumento> actualizar(@PathVariable String tipo,
            @RequestBody PlantillaUpdateRequest datos, HttpServletRequest request) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        PlantillaDocumento actualizada = plantillaService.actualizar(tipo, datos.getContenido(), usuario);
        bitacoraService.registrar(usuario, "Actualización de plantilla: " + tipo, request);
        return ResponseEntity.ok(actualizada);
    }
}
