package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.entity.QuejaEvidenciaArchivo;
import ipn.escom.defensoria.primercontacto.service.EvidenciaPrimerContactoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/primer-contacto/evidencias")
public class EvidenciaPrimerContactoController {

    private final EvidenciaPrimerContactoService evidenciaService;

    public EvidenciaPrimerContactoController(
            EvidenciaPrimerContactoService evidenciaService
    ) {
        this.evidenciaService = evidenciaService;
    }

    /*
     * Abre la evidencia original de la queja (CU-PC-06/07).
     * {id} es el id de la evidencia tal como viene en el expediente.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long id
    ) {

        QuejaEvidenciaArchivo archivo = evidenciaService.obtener(id);

        MediaType tipo;
        try {
            tipo = archivo.getTipoMime() != null
                    ? MediaType.parseMediaType(archivo.getTipoMime())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (RuntimeException ex) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }

        String nombre = archivo.getNombreArchivo() != null
                ? archivo.getNombreArchivo()
                : "evidencia-" + id;

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(nombre, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(archivo.getContenido());
    }
}
