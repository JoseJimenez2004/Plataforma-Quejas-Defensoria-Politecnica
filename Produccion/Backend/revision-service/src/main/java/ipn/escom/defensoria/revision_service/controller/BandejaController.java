package ipn.escom.defensoria.revision_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.model.BandejaResumenModel;
import ipn.escom.defensoria.revision_service.service.RevisionQuejaService;

/** "Bandeja de Entrada" del recepcionista: contadores + tabla de quejas pendientes/en proceso. */
@RestController
@RequestMapping("/api/revision/bandeja")
@PreAuthorize("hasRole('RECEPCIONISTA')")
@Tag(name = "Bandeja", description = "Panel de gestión de recepción")
public class BandejaController {

    private final RevisionQuejaService revisionService;

    public BandejaController(RevisionQuejaService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping
    @Operation(summary = "Contadores (pendientes/en proceso/turnadas hoy) y lista de quejas por trabajar")
    public ResponseEntity<BandejaResumenModel> bandeja() {
        return ResponseEntity.ok(revisionService.bandeja());
    }
}
