package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import ipn.escom.defensoria.primercontacto.service.RemisionExternaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/primer-contacto/remisiones")
public class RemisionExternaController {

    private final RemisionExternaService remisionExternaService;
    private final AnalistaAutenticadoService analistaAutenticadoService;

    public RemisionExternaController(
            RemisionExternaService remisionExternaService, AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.remisionExternaService =
                remisionExternaService;
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    @PostMapping
    public ResponseEntity<RemisionDTO> crearRemision(
            @Valid @RequestBody CrearRemisionDTO dto,
            Authentication authentication
    ) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return ResponseEntity.ok(
                remisionExternaService.crearRemision(
                        dto,
                        analista
                )
        );
    }

    @GetMapping("/expediente/{expedienteId}")
    public RemisionDTO obtenerPorExpediente(
            @PathVariable Long expedienteId
    ) {

        return remisionExternaService
                .obtenerPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public RemisionDTO obtenerPorFolio(
            @PathVariable String folio
    ) {

        return remisionExternaService
                .obtenerPorFolio(folio);
    }

    /*
     * Oficio de remisión en PDF (CU-PC-09).
     */
    @GetMapping("/folio/{folio}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable String folio
    ) {

        RemisionExternaService.OficioPdf oficio =
                remisionExternaService.generarPdf(folio);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(oficio.nombreArchivo(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(oficio.contenido());
    }

    /*
     * Registra que el oficio ya se envió: remisión ENVIADA y
     * expediente REMITIDA.
     */
    @PutMapping("/folio/{folio}/enviar")
    public RemisionDTO enviarRemision(
            @PathVariable String folio,
            Authentication authentication
    ) {

        analistaAutenticadoService.obtenerAnalista(authentication);

        return remisionExternaService
                .enviarRemision(folio);
    }
}
