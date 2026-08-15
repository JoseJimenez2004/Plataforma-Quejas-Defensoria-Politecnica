package ipn.escom.defensoria.catalogo_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.catalogo_service.dto.DependenciaRequest;
import ipn.escom.defensoria.catalogo_service.dto.ImportacionResumenModel;
import ipn.escom.defensoria.catalogo_service.entity.Dependencia;
import ipn.escom.defensoria.catalogo_service.service.DependenciaService;

/**
 * CRUD administrativo del catálogo -- a diferencia de DependenciaController (público,
 * solo lectura), esto exige un JWT con rol ADMIN_SISTEMAS emitido por admin-service (mismo
 * jwt.secret compartido entre microservicios, ver JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/catalogos/dependencias/admin")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Dependencias (Admin)", description = "Alta, edición e importación masiva del catálogo")
public class DependenciaAdminController {

    @Autowired
    private DependenciaService dependenciaService;

    @GetMapping
    @Operation(summary = "Lista TODAS las dependencias (activas e inactivas) para el panel de administración")
    public ResponseEntity<List<Dependencia>> listarTodas() {
        return ResponseEntity.ok(dependenciaService.listarTodas());
    }

    @PostMapping
    @Operation(summary = "Crea una nueva dependencia/unidad académica")
    public ResponseEntity<Dependencia> crear(@RequestBody DependenciaRequest datos) {
        return ResponseEntity.ok(dependenciaService.crear(datos));
    }

    @PutMapping("/{clave}")
    @Operation(summary = "Edita una dependencia existente por su clave")
    public ResponseEntity<Dependencia> editar(@PathVariable String clave, @RequestBody DependenciaRequest datos) {
        return ResponseEntity.ok(dependenciaService.editar(clave, datos));
    }

    @PostMapping(value = "/importar-excel", consumes = "multipart/form-data")
    @Operation(summary = "Importa/actualiza dependencias masivamente desde un archivo Excel (SIA/IPN)")
    public ResponseEntity<ImportacionResumenModel> importarExcel(@RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(dependenciaService.importarDesdeExcel(archivo));
    }
}
