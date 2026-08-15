package ipn.escom.defensoria.revision_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.model.AreaOpcionModel;
import ipn.escom.defensoria.revision_service.model.DefensorOpcionModel;
import ipn.escom.defensoria.revision_service.service.CatalogoRefService;
import ipn.escom.defensoria.revision_service.service.PersonalRefService;

/** Combos usados en "Búsqueda de Antecedentes y Turnado" y "Registro Manual". */
@RestController
@RequestMapping("/api/revision/catalogos")
@PreAuthorize("hasRole('RECEPCIONISTA')")
@Tag(name = "Catálogos (Revisión)", description = "Opciones de área y defensor para canalizar una queja")
public class CatalogosRevisionController {

    @Autowired
    private CatalogoRefService catalogoRefService;

    @Autowired
    private PersonalRefService personalRefService;

    @GetMapping("/areas")
    @Operation(summary = "Lista de dependencias/unidades académicas (catalogo-service) para el combo de área")
    public ResponseEntity<List<AreaOpcionModel>> areas() {
        return ResponseEntity.ok(catalogoRefService.listarAreas());
    }

    @GetMapping("/defensores")
    @Operation(summary = "Personal con rol Defensor o Subdefensor disponible para asignar")
    public ResponseEntity<List<DefensorOpcionModel>> defensores() {
        return ResponseEntity.ok(personalRefService.listarDefensoresDisponibles());
    }
}
