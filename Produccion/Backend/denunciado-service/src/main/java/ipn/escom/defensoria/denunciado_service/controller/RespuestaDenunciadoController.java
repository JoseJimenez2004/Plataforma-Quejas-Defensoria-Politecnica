package ipn.escom.defensoria.denunciado_service.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.denunciado_service.dto.RegistroRespuestaRequest;
import ipn.escom.defensoria.denunciado_service.dto.RespuestaDetalleModel;
import ipn.escom.defensoria.denunciado_service.dto.RespuestaRegistradaModel;
import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciadoArchivo;
import ipn.escom.defensoria.denunciado_service.service.RespuestaDenunciadoService;

@RestController
@RequestMapping("/api/denunciado/respuestas")
@Tag(name = "Respuesta del denunciado")
public class RespuestaDenunciadoController {

    private static final String ROLES_PERSONAL =
            "hasAnyRole('RECEPCIONISTA','ANALISTA_PRIMER_CONTACTO','SUBDEFENSOR','DEFENSOR')";

    private final RespuestaDenunciadoService service;

    public RespuestaDenunciadoController(RespuestaDenunciadoService service) {
        this.service = service;
    }

    /*
     * PC-2 (2026-11): ya no hay formulario público; este POST exige sesión (WebConfig ya no lo
     * deja abierto) y queda solo por compatibilidad. Lo normal es /personal.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ROLES_PERSONAL)
    @Operation(summary = "Registro con los datos del denunciado (requiere sesión; ya no es público)")
    public ResponseEntity<RespuestaRegistradaModel> registrar(@ModelAttribute RegistroRespuestaRequest datos) {
        return ResponseEntity.ok(service.registrar(datos));
    }

    /** PC-2: el analista de Primer Contacto captura la versión del denunciado. */
    @PostMapping(value = "/personal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALISTA_PRIMER_CONTACTO')")
    @Operation(summary = "El analista de Primer Contacto registra la versión del denunciado (entrevista)")
    public ResponseEntity<RespuestaRegistradaModel> registrarPorPersonal(@ModelAttribute RegistroRespuestaRequest datos,
                                                                         Authentication autenticacion) {
        return ResponseEntity.ok(service.registrarPorPersonal(datos, autenticacion.getName()));
    }

    @GetMapping
    @PreAuthorize(ROLES_PERSONAL)
    @Operation(summary = "Respuestas del denunciado de una queja (personal de la Defensoría)")
    public ResponseEntity<List<RespuestaDetalleModel>> listar(@RequestParam String folioQueja) {
        return ResponseEntity.ok(service.listarPorFolioQueja(folioQueja));
    }

    @GetMapping("/archivos/{id}")
    @PreAuthorize(ROLES_PERSONAL)
    @Operation(summary = "Muestra en el navegador una credencial o evidencia (personal de la Defensoría)")
    public ResponseEntity<byte[]> archivo(@PathVariable Long id) {
        RespuestaDenunciadoArchivo a = service.obtenerArchivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getTipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(a.getNombreArchivo(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(a.getContenido());
    }
}
