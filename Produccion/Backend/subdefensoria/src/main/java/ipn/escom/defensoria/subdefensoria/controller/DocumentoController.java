package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.service.DocumentoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Descarga de los PDF de Subdefensoría (formato de ejemplo, ver DocumentoPdfService).
 *
 *   GET /api/subdefensoria/documentos/oficio/{oficioId}
 *   GET /api/subdefensoria/documentos/recordatorio/{recordatorioId}
 *   GET /api/subdefensoria/documentos/acuerdo-conciliacion/{expedienteId}
 *   GET /api/subdefensoria/documentos/acuerdo-conclusion/{expedienteId}
 *
 * Y para el quejoso, desde el enlace de su acuerdo (sin sesión, ruta pública):
 *
 *   GET /api/subdefensoria/publico/acuerdos/{token}/pdf?tipo=conciliacion|conclusion
 */
@RestController
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/api/subdefensoria/documentos/oficio/{oficioId}")
    public ResponseEntity<byte[]> oficio(@PathVariable Long oficioId) {
        return pdf(documentoService.oficio(oficioId));
    }

    @GetMapping("/api/subdefensoria/documentos/recordatorio/{recordatorioId}")
    public ResponseEntity<byte[]> recordatorio(@PathVariable Long recordatorioId) {
        return pdf(documentoService.recordatorio(recordatorioId));
    }

    @GetMapping("/api/subdefensoria/documentos/acuerdo-conciliacion/{expedienteId}")
    public ResponseEntity<byte[]> acuerdoConciliacion(@PathVariable Long expedienteId) {
        return pdf(documentoService.acuerdoConciliacion(expedienteId));
    }

    @GetMapping("/api/subdefensoria/documentos/acuerdo-conclusion/{expedienteId}")
    public ResponseEntity<byte[]> acuerdoConclusion(@PathVariable Long expedienteId) {
        return pdf(documentoService.acuerdoConclusion(expedienteId));
    }

    @GetMapping("/api/subdefensoria/publico/acuerdos/{token}/pdf")
    public ResponseEntity<byte[]> acuerdoDelQuejoso(
            @PathVariable String token,
            @RequestParam(defaultValue = "conciliacion") String tipo
    ) {
        return pdf(documentoService.acuerdoPorToken(token, "conclusion".equalsIgnoreCase(tipo)));
    }

    private ResponseEntity<byte[]> pdf(DocumentoService.Pdf pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nombreArchivo()).build().toString())
                .body(pdf.contenido());
    }
}
