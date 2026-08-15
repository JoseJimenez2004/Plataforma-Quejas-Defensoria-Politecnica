package ipn.escom.defensoria.revision_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.revision_service.model.AntecedenteModel;
import ipn.escom.defensoria.revision_service.model.QuejaDetalleModel;
import ipn.escom.defensoria.revision_service.model.RechazarQuejaRequest;
import ipn.escom.defensoria.revision_service.model.TurnarQuejaRequest;
import ipn.escom.defensoria.revision_service.service.RevisionQuejaService;

/** "Validación de Requisitos", "Emisión de Notificación de Rechazo" y "Búsqueda de
 * Antecedentes y Turnado" -- las 3 pantallas que operan sobre una queja puntual por folio. */
@RestController
@RequestMapping("/api/revision/quejas")
@PreAuthorize("hasRole('RECEPCIONISTA')")
@Tag(name = "Quejas (Revisión)", description = "Validación, rechazo y canalización de una queja")
public class QuejaRevisionController {

    @Autowired
    private RevisionQuejaService revisionService;

    @GetMapping("/{folio}")
    @Operation(summary = "Detalle de una queja para validarla (resumen + documentos adjuntos)")
    public ResponseEntity<QuejaDetalleModel> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(revisionService.detalle(folio));
    }

    @GetMapping("/{folio}/antecedentes")
    @Operation(summary = "Otras quejas previas de la misma persona (detección de duplicidad)")
    public ResponseEntity<List<AntecedenteModel>> antecedentes(@PathVariable String folio) {
        return ResponseEntity.ok(revisionService.antecedentes(folio));
    }

    @GetMapping("/evidencias/{id}")
    @Operation(summary = "Descarga un documento adjunto de la queja")
    public ResponseEntity<byte[]> descargarEvidencia(@PathVariable Long id) {
        QuejaEvidencia evidencia = revisionService.obtenerEvidencia(id);
        MediaType tipo = evidencia.getTipoMime() != null
                ? MediaType.parseMediaType(evidencia.getTipoMime())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + evidencia.getNombreArchivo() + "\"")
                .body(evidencia.getContenido());
    }

    @PostMapping("/{folio}/rechazar")
    @Operation(summary = "Rechaza la queja por documentación incompleta y notifica al quejoso por correo")
    public ResponseEntity<Map<String, String>> rechazar(
            @PathVariable String folio,
            @RequestBody RechazarQuejaRequest datos,
            Authentication authentication) {
        Queja queja = revisionService.rechazar(folio, datos.getMotivos(), datos.getObservaciones(), authentication.getName());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Rechazo enviado correctamente.",
                "estatus", queja.getEstatus()));
    }

    @PostMapping("/{folio}/turnar")
    @Operation(summary = "Canaliza la queja a un área/defensor y genera el folio oficial de turnado")
    public ResponseEntity<Map<String, String>> turnar(
            @PathVariable String folio,
            @RequestBody TurnarQuejaRequest datos,
            Authentication authentication) {
        Queja queja = revisionService.turnar(
                folio, datos.getAreaTurnada(), datos.getDefensorAsignado(), datos.getComentarios(),
                authentication.getName());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Queja turnada correctamente.",
                "estatus", queja.getEstatus(),
                "numeroFolio", queja.getNumeroFolio()));
    }
}
