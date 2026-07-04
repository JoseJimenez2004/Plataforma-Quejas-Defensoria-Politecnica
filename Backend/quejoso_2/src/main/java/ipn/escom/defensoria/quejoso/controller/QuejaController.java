package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.EvidenciaDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaRegistroDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaSeguimientoDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaEditarDTO;
import ipn.escom.defensoria.quejoso.entity.EvidenciaEntity;
import ipn.escom.defensoria.quejoso.entity.Queja;
import ipn.escom.defensoria.quejoso.repository.EvidenciaRepository;
import ipn.escom.defensoria.quejoso.repository.QuejaRepository;
import ipn.escom.defensoria.quejoso.service.QuejaService;
import ipn.escom.defensoria.quejoso.storage.StorageService;
import ipn.escom.defensoria.quejoso.entity.Tutor;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/quejoso/quejas")
public class QuejaController {

    @Autowired
    private QuejaService quejaService;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private EvidenciaRepository evidenciaRepository;

    @Autowired
    private StorageService storageService;

    @Value("${app.base-url:http://2.25.78.22:8083}")
    private String baseUrl;

    @PostMapping(value = "/registrar", consumes = {"multipart/form-data"})
    public ResponseEntity<QuejaSeguimientoDTO> registrarQueja(
            @RequestPart("datos") QuejaRegistroDTO registroDTO,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos) {

        Queja nuevaQueja = mappearDtoAEntidad(registroDTO);
        Queja quejaGuardada = quejaService.registrarQueja(
                nuevaQueja,
                registroDTO.getIdentificacionInstitucional(),
                registroDTO.getCorreo(),
                archivos
        );

        QuejaSeguimientoDTO respuesta = mappearAseguimientoDTO(quejaGuardada);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    private QuejaSeguimientoDTO mappearAseguimientoDTO(Queja queja) {
        return QuejaSeguimientoDTO.builder()
                .folio(queja.getFolio())
                .asunto(queja.getAsunto())
                .descripcionHechos(queja.getDescripcion())
                .fechaRegistro(queja.getFechaRegistro())
                .estatusActual(queja.getEstatus())
                .evidencias(queja.getEvidencias() != null ?
                        queja.getEvidencias().stream()
                                .map(e -> new EvidenciaDTO(
                                        e.getId(),
                                        e.getNombreArchivo(),
                                        baseUrl + "/api/quejoso/quejas/evidencias/" + e.getId()))
                                .collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

    @GetMapping("/evidencias/{id}/publico")
    public ResponseEntity<byte[]> descargarPublico(
            @PathVariable Long id,
            @RequestParam String folio,
            @RequestParam String correo) {

        EvidenciaEntity evidencia = evidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evidencia no encontrada"));

        Queja queja = evidencia.getQueja();
        if (!queja.getFolio().equals(folio) || !queja.getCorreoQuejoso().equalsIgnoreCase(correo)) {
            throw new RuntimeException("No tienes permiso para descargar este archivo");
        }

        return construirRespuestaDescarga(evidencia);
    }

    @GetMapping("/evidencias/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EvidenciaEntity evidencia = evidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evidencia no encontrada"));

        if (evidencia.getQueja().getQuejoso() == null || 
            !evidencia.getQueja().getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para descargar este archivo");
        }

        return construirRespuestaDescarga(evidencia);
    }

    private ResponseEntity<byte[]> construirRespuestaDescarga(EvidenciaEntity evidencia) {
        byte[] contenido = storageService.leer(evidencia.getUrlAlmacenamiento());
        String mime = evidencia.getTipoContenido() != null ? evidencia.getTipoContenido() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + evidencia.getNombreArchivo() + "\"")
                .body(contenido);
    }

    @GetMapping("/seguimiento/publico")
    public ResponseEntity<?> consultarSeguimientoPublico(@RequestParam String folio, @RequestParam String correo) {
        return quejaService.obtenerSeguimiento(folio, correo)
                .map(dto -> {
                    String urlBase = baseUrl + "/api/quejoso/quejas/evidencias/";
                    dto.getEvidencias().forEach(e -> e.setUrlDescarga(
                            urlBase + e.getId() + "/publico?folio=" + folio + "&correo=" + correo));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/seguimiento/privado/{folio}")
    public ResponseEntity<QuejaSeguimientoDTO> consultarSeguimientoPrivado(@PathVariable String folio) {
        return quejaService.obtenerSeguimiento(folio)
                .map(dto -> {
                    String urlBase = baseUrl + "/api/quejoso/quejas/evidencias/";
                    dto.getEvidencias().forEach(e -> e.setUrlDescarga(urlBase + e.getId()));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping(value = "/editar/{folio}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> editar(@PathVariable String folio, @RequestPart("datos") QuejaEditarDTO editarDTO, 
                                    @RequestPart(value = "nuevosArchivos", required = false) List<MultipartFile> archivos) {
        try {
            Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            quejaService.editarQueja(folio, usuarioAutenticado.getId(), editarDTO, archivos);
            return ResponseEntity.ok("Queja actualizada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/borrar/{folio}")
    public ResponseEntity<?> eliminar(@PathVariable String folio) {
        try {
            Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            quejaService.eliminarQueja(folio, usuarioAutenticado.getId());
            return ResponseEntity.ok("La queja ha sido cancelada exitosamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    private Queja mappearDtoAEntidad(QuejaRegistroDTO dto) {
        Queja q = new Queja();
        q.setAsunto(dto.getAsunto());
        q.setDescripcion(dto.getDescripcionHechos());
        q.setUnidaddondeOcurrio(dto.getUnidadAcademica());
        q.setFechaHechos(dto.getFechaHechos());
        q.setNombreDenunciado(dto.getNombreDenunciado() + " " + dto.getPrimerApellidoDenunciado());
        q.setCorreoQuejoso(dto.getCorreo());
        q.setNombreQuejoso(dto.getNombreQuejoso() + " " + dto.getPrimerApellido() + " " + (dto.getSegundoApellido() != null ? dto.getSegundoApellido() : ""));
        q.setIdentificacionInstitucional(dto.getIdentificacionInstitucional());
        q.setTipoIdentificacion(dto.getTipoIdentificacion());
        q.setFechaNacimiento(dto.getFechaNacimiento());

        if (dto.getTutor() != null) {
            Tutor t = new Tutor();
            t.setNombre(dto.getTutor().getNombre());
            t.setPrimerApellido(dto.getTutor().getPrimerApellido());
            t.setSegundoApellido(dto.getTutor().getSegundoApellido());
            t.setParentesco(dto.getTutor().getParentesco());
            t.setCorreo(dto.getTutor().getCorreo());
            t.setTelefono(dto.getTutor().getTelefono());
            t.setQueja(q);
            q.setTutor(t);
        }
        return q;
    }

    // Métodos para descargar acuse omitidos por brevedad, pero la lógica sigue igual...
}