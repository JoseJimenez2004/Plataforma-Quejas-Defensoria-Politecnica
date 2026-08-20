package ipn.escom.defensoria.revision_service.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.model.RegistroManualResponse;
import ipn.escom.defensoria.revision_service.service.RevisionQuejaService;

/** "Registro Manual de Nueva Queja" -- alta de documentos físicos recibidos en papel. */
@RestController
@RequestMapping("/api/revision/registro-manual")
@PreAuthorize("hasRole('RECEPCIONISTA')")
@Tag(name = "Registro Manual", description = "Alta de quejas recibidas en documento físico")
public class RegistroManualController {

    private final RevisionQuejaService revisionService;

    public RegistroManualController(RevisionQuejaService revisionService) {
        this.revisionService = revisionService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Registra una queja recibida en papel y genera su folio de seguimiento")
    public ResponseEntity<RegistroManualResponse> registrar(
            @RequestParam String nombre,
            @RequestParam String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String tipoUsuario,
            @RequestParam(required = false) String dependenciaClave,
            @RequestParam(required = false) String numeroOficio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaRecepcionFisica,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam String descripcion,
            @RequestParam(required = false) String ubicacionFisica,
            @RequestParam(required = false) MultipartFile archivo,
            Authentication authentication) {

        Queja creada = revisionService.registrarManual(
                nombre, apellidoPaterno, apellidoMaterno, tipoUsuario, dependenciaClave, numeroOficio,
                fechaRecepcionFisica, tipoDocumento, descripcion, ubicacionFisica, archivo,
                authentication.getName());

        return ResponseEntity.ok(new RegistroManualResponse(
                creada.getNumeroFolio(), "Entrada registrada. Folio de seguimiento: " + creada.getNumeroFolio()));
    }
}
