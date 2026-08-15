package ipn.escom.defensoria.revision_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.revision_service.model.CrearConciliacionRequest;
import ipn.escom.defensoria.revision_service.service.ConciliacionRevisionService;

/** Emisión de acuerdos de conciliación por parte del personal. El quejoso solo los lee y
 * responde (aceptar/rechazar) desde queja-service ("/api/quejoso/conciliaciones/..."). */
@RestController
@RequestMapping("/api/revision/conciliaciones")
@PreAuthorize("hasAnyRole('RECEPCIONISTA','SUBDEFENSOR','DEFENSOR','ADMIN_SISTEMAS')")
@Tag(name = "Conciliación (Revisión)", description = "Emisión de acuerdos de conciliación al quejoso")
public class ConciliacionRevisionController {

    @Autowired
    private ConciliacionRevisionService conciliacionService;

    @PostMapping
    @Operation(summary = "Emite un nuevo acuerdo de conciliación para una queja por folio")
    public ResponseEntity<AcuerdoConciliacion> crear(
            @RequestBody CrearConciliacionRequest datos,
            Authentication authentication) {
        return ResponseEntity.ok(conciliacionService.crear(datos, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Lista los acuerdos de conciliación emitidos (opcionalmente filtrados por folio)")
    public ResponseEntity<List<AcuerdoConciliacion>> listar(
            @RequestParam(required = false) String folio) {
        return ResponseEntity.ok(conciliacionService.listar(folio));
    }
}
