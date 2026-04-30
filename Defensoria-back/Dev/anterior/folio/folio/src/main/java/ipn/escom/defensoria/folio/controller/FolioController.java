package ipn.escom.defensoria.folio.controller;

import ipn.escom.defensoria.folio.dto.FolioDTO;
import ipn.escom.defensoria.folio.entity.FolioEntity;
import ipn.escom.defensoria.folio.service.FolioServiece;
import ipn.escom.defensoria.folio.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/consulta")
@CrossOrigin(origins = "http://localhost:4200")

public class FolioController {
    @Autowired
    private FolioServiece service;
    @Autowired
    private PdfService pdfService;
        // En FolioController.java
    @PostMapping(value = "/generar", consumes = {"multipart/form-data"})
    public ResponseEntity<String> generar(
        @RequestPart("formulario") FolioDTO formulario,
        @RequestPart("archivos") List<MultipartFile> archivos) {

        return ResponseEntity.ok(service.generadorFolio(formulario, archivos));
    }
    
    @PostMapping(value = "/{codigo}/evidencias/extra", consumes = {"multipart/form-data"})
    public ResponseEntity<String> subirEvidenciaExtra(
            @PathVariable String codigo,
            @RequestPart("archivos") List<MultipartFile> archivos) {

        return ResponseEntity.ok(service.guardarEvidenciasExtra(codigo, archivos));
    }
    
    @GetMapping("/{codigo}/descargar-acuse")
    public void descargarAcuse(@PathVariable String codigo, HttpServletResponse response) throws IOException, com.lowagie.text.DocumentException {
        FolioEntity folio = service.buscarPorCodigo(codigo);

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Acuse_" + codigo + ".pdf";
        response.setHeader(headerKey, headerValue);

        pdfService.generarAcuse(folio, response);
    }
    
}
