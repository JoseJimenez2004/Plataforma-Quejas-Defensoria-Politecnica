package ipn.escom.defensoria.revision_service.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.model.HistorialItemModel;
import ipn.escom.defensoria.revision_service.service.HistorialExportService;
import ipn.escom.defensoria.revision_service.service.RevisionQuejaService;

/** "Historial de Trámites Recibidos" -- quejas ya rechazadas o turnadas, con filtros y
 * exportación a Excel. */
@RestController
@RequestMapping("/api/revision/historial")
@PreAuthorize("hasRole('RECEPCIONISTA')")
@Tag(name = "Historial", description = "Trámites ya procesados (turnados o rechazados)")
public class HistorialController {

    private final RevisionQuejaService revisionService;
    private final HistorialExportService exportService;

    public HistorialController(RevisionQuejaService revisionService, HistorialExportService exportService) {
        this.revisionService = revisionService;
        this.exportService = exportService;
    }

    @GetMapping
    @Operation(summary = "Lista el historial, opcionalmente filtrado por texto libre, estatus y fecha")
    public ResponseEntity<List<HistorialItemModel>> historial(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(revisionService.historial(texto, estatus, fecha));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta el historial filtrado a un archivo Excel (.xlsx)")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<HistorialItemModel> items = revisionService.historial(texto, estatus, fecha);
        byte[] archivo = exportService.exportar(items);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historial-tramites.xlsx\"")
                .body(archivo);
    }
}
