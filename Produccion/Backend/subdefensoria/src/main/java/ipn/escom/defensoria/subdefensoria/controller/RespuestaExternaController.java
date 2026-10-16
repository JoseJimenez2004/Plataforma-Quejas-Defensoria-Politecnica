package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.entity.RespuestaArchivo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import ipn.escom.defensoria.subdefensoria.dto.RegistrarRespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.dto.RespuestaExternaDTO;
import ipn.escom.defensoria.subdefensoria.service.RespuestaExternaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/respuestas-externas")
public class RespuestaExternaController {

    private final RespuestaExternaService respuestaExternaService;

    public RespuestaExternaController(RespuestaExternaService respuestaExternaService) {
        this.respuestaExternaService = respuestaExternaService;
    }

    /** Boton "Confirmar Registro y Habilitar Dictamen" en P15. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RespuestaExternaDTO registrarRespuesta(@Valid @RequestBody RegistrarRespuestaExternaDTO dto) {
        return respuestaExternaService.registrarRespuesta(dto);
    }

    /*
     * Lo mismo, con el archivo de la respuesta (PDF, JPG o PNG, máx. 10 MB) en una sola
     * petición multipart: parte "datos" (JSON) y parte "archivo" (opcional).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaExternaDTO registrarConArchivo(
            @Valid @RequestPart("datos") RegistrarRespuestaExternaDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo
    ) {
        return respuestaExternaService.registrarRespuesta(dto, archivo);
    }

    /* Adjuntar o reemplazar el archivo de una respuesta ya registrada. */
    @PostMapping(value = "/{respuestaId}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaExternaDTO adjuntarArchivo(@PathVariable Long respuestaId,
                                               @RequestPart("archivo") MultipartFile archivo) {
        return respuestaExternaService.adjuntarArchivo(respuestaId, archivo);
    }

    /* Respuestas registradas de un oficio (detalle del historial de oficios). */
    @GetMapping("/oficio/{oficioId}")
    public List<RespuestaExternaDTO> porOficio(@PathVariable Long oficioId) {
        return respuestaExternaService.porOficio(oficioId);
    }

    @GetMapping("/{respuestaId}/archivo")
    public ResponseEntity<byte[]> archivo(@PathVariable Long respuestaId) {
        RespuestaArchivo archivo = respuestaExternaService.archivo(respuestaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.getNombreArchivo(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.getContenido());
    }
}
