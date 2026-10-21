package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearNotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.NotaAnalisisService;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/primer-contacto/notas")
public class NotaAnalisisController {

    private final NotaAnalisisService notaAnalisisService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public NotaAnalisisController(
            NotaAnalisisService notaAnalisisService,
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.notaAnalisisService = notaAnalisisService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping
    public ResponseEntity<NotaAnalisisDTO> crearNota(
            @Valid @RequestBody CrearNotaAnalisisDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                notaAnalisisService.crearNota(
                        dto,
                        analista
                )
        );
    }

    @GetMapping("/expediente/{expedienteId}")
    public List<NotaAnalisisDTO> listarPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return notaAnalisisService
                .listarPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public List<NotaAnalisisDTO> listarPorFolio(
            @PathVariable String folio
    ) {
        return notaAnalisisService.listarPorFolio(folio);
    }

    /*
     * Oficio en PDF (la nota de análisis es el oficio del expediente). Se abre en el
     * navegador (inline) para verlo al guardarlo; desde ahí se puede descargar.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> oficioPdf(
            @PathVariable Long id
    ) {

        NotaAnalisisService.OficioPdf oficio = notaAnalisisService.generarPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(oficio.nombreArchivo(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(oficio.contenido());
    }

    @PutMapping("/{id}")
    public NotaAnalisisDTO actualizarNota(
            @PathVariable Long id,
            @Valid @RequestBody CrearNotaAnalisisDTO dto,
            Authentication authentication
    ) {
        return notaAnalisisService.actualizarNota(
                id,
                dto,
                analistaAutenticadoService.obtenerAnalista(authentication)
        );
    }

    @DeleteMapping("/{id}")
    public void eliminarNota(
            @PathVariable Long id,
            Authentication authentication
    ) {
        notaAnalisisService.eliminarNota(
                id,
                analistaAutenticadoService.obtenerAnalista(authentication)
        );
    }
}