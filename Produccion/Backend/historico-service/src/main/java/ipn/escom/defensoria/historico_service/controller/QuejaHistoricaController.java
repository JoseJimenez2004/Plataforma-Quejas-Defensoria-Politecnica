package ipn.escom.defensoria.historico_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.historico_service.dto.CapturaQuejaHistoricaRequest;
import ipn.escom.defensoria.historico_service.dto.EvidenciaResumenDTO;
import ipn.escom.defensoria.historico_service.dto.QuejaHistoricaDTO;
import ipn.escom.defensoria.historico_service.entity.EvidenciaHistorica;
import ipn.escom.defensoria.historico_service.entity.QuejaHistorica;
import ipn.escom.defensoria.historico_service.service.QuejaHistoricaService;
import jakarta.validation.Valid;

/**
 * Captura y consulta de quejas históricas desde el panel de Recepción.
 *
 * Todo aquí exige sesión con rol RECEPCIONISTA o ADMIN_SISTEMAS: son datos de personas
 * reales y el histórico no tiene por qué ser público.
 */
@RestController
@RequestMapping("/api/historico/quejas")
public class QuejaHistoricaController {

    private final QuejaHistoricaService service;

    public QuejaHistoricaController(QuejaHistoricaService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<QuejaHistoricaDTO> capturar(
            @Valid @RequestBody CapturaQuejaHistoricaRequest datos,
            Authentication authentication) {

        String capturadoPor = authentication != null ? authentication.getName() : null;
        QuejaHistorica guardada = service.capturar(datos, capturadoPor);
        return ResponseEntity.ok(service.aDTO(guardada));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<Page<QuejaHistoricaDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String unidadAcademicaClave,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Page<QuejaHistoricaDTO> resultado = service
                .listar(texto, unidadAcademicaClave, PageRequest.of(pagina, tamanio))
                .map(service::aDTO);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{folio}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<QuejaHistoricaDTO> obtener(@PathVariable String folio) {
        return ResponseEntity.ok(service.aDTO(service.obtenerPorFolio(folio)));
    }

    // ------------------------------------------------------------- evidencias

    @PostMapping(value = "/{folio}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<Map<String, Object>> subirEvidencia(
            @PathVariable String folio,
            @RequestParam MultipartFile archivo,
            @RequestParam(required = false) String tipo) {

        EvidenciaHistorica guardada = service.agregarEvidencia(folio, archivo, tipo);
        return ResponseEntity.ok(Map.of(
                "id", guardada.getId(),
                "nombreArchivo", guardada.getNombreArchivo(),
                "mensaje", "Evidencia agregada a la queja histórica " + folio));
    }

    @GetMapping("/{folio}/evidencias")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<List<EvidenciaResumenDTO>> listarEvidencias(@PathVariable String folio) {
        return ResponseEntity.ok(service.listarEvidencias(folio));
    }

    @GetMapping("/evidencias/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN_SISTEMAS')")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable Long id) {
        EvidenciaHistorica evidencia = service.obtenerEvidencia(id);

        MediaType tipo = evidencia.getTipoMime() != null
                ? MediaType.parseMediaType(evidencia.getTipoMime())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + evidencia.getNombreArchivo() + "\"")
                .body(new ByteArrayResource(evidencia.getContenido()));
    }
}
