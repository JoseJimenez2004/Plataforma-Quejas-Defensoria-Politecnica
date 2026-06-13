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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    /** URL base del backend, configurable en application.properties. */
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Endpoint para el Mockup MQ-02 y MQ-03
     * Registra la queja, genera folio y vincula tutor si es necesario.
     */
    @PostMapping(value = "/registrar", consumes = {"multipart/form-data"})
    public ResponseEntity<QuejaSeguimientoDTO> registrarQueja(
            @RequestPart("datos") QuejaRegistroDTO registroDTO,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos) {

        // 1. Convertimos el DTO que viene en el JSON a nuestra Entidad
        Queja nuevaQueja = mappearDtoAEntidad(registroDTO);

        // 2. Registrar queja: genera folio, vincula quejoso y guarda evidencias en disco + BD
        Queja quejaGuardada = quejaService.registrarQueja(
                nuevaQueja,
                registroDTO.getIdentificacionInstitucional(),
                registroDTO.getCorreo(),
                archivos // el servicio guarda los archivos después de asignar el folio
        );

        // 4. Mapeamos a la respuesta que espera el Mockup MQ-04
        QuejaSeguimientoDTO respuesta = mappearAseguimientoDTO(quejaGuardada);

        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // Dentro de QuejaController.java

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
                                .collect(java.util.stream.Collectors.toList())
                        : new java.util.ArrayList<>())
                .build();
    }

    /**
     * Descarga pública de una evidencia. Requiere el folio y correo del quejoso
     * para validar la propiedad sin necesidad de autenticación JWT.
     * Usado desde el modal de seguimiento público en la página de inicio.
     */
    @GetMapping("/evidencias/{id}/publico")
    public ResponseEntity<byte[]> descargarPublico(
            @PathVariable Long id,
            @RequestParam String folio,
            @RequestParam String correo) {

        EvidenciaEntity evidencia = evidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evidencia no encontrada"));

        Queja queja = evidencia.getQueja();
        boolean folioCoincide = queja.getFolio().equals(folio);
        boolean correoCoincide = queja.getCorreoQuejoso() != null
                && queja.getCorreoQuejoso().equalsIgnoreCase(correo);

        if (!folioCoincide || !correoCoincide) {
            throw new RuntimeException("No tienes permiso para descargar este archivo");
        }

        return construirRespuestaDescarga(evidencia);
    }

    /**
     * Descarga autenticada de una evidencia. El JWT del quejoso se valida
     * mediante el interceptor de Angular y el filtro JWT de Spring Security.
     * Usado desde el detalle de queja en el dashboard.
     */
    @GetMapping("/evidencias/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        EvidenciaEntity evidencia = evidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evidencia no encontrada"));

        Queja queja = evidencia.getQueja();
        if (queja.getQuejoso() == null
                || !queja.getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para descargar este archivo");
        }

        return construirRespuestaDescarga(evidencia);
    }

    /** Lógica de descarga compartida: lee los bytes del storage y los devuelve con headers correctos. */
    private ResponseEntity<byte[]> construirRespuestaDescarga(EvidenciaEntity evidencia) {
        byte[] contenido = storageService.leer(evidencia.getUrlAlmacenamiento());
        String mime = evidencia.getTipoContenido() != null
                ? evidencia.getTipoContenido()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + evidencia.getNombreArchivo() + "\"")
                .body(contenido);
    }


    // VÍA 1: CONSULTA RÁPIDA (Pública - MQ-06)
    @GetMapping("/seguimiento/publico")
    public ResponseEntity<?> consultarSeguimientoPublico(
            @RequestParam String folio,
            @RequestParam String correo) {

        return quejaService.obtenerSeguimiento(folio, correo)
                .map(dto -> {
                    // URL pública: incluye folio y correo como parámetros de validación
                    String urlBase = baseUrl + "/api/quejoso/quejas/evidencias/";
                    dto.getEvidencias().forEach(e -> e.setUrlDescarga(
                            urlBase + e.getId() + "/publico?folio=" + folio + "&correo=" + correo));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // VÍA 2: SEGUIMIENTO DETALLADO (Privada - JWT requerido)
    @GetMapping("/seguimiento/privado/{folio}")
    public ResponseEntity<QuejaSeguimientoDTO> consultarSeguimientoPrivado(@PathVariable String folio) {
        return quejaService.obtenerSeguimiento(folio)
                .map(dto -> {
                    // URL privada: el interceptor de Angular agrega el JWT automáticamente
                    String urlBase = baseUrl + "/api/quejoso/quejas/evidencias/";
                    dto.getEvidencias().forEach(e -> e.setUrlDescarga(urlBase + e.getId()));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @PutMapping(value = "/editar/{folio}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> editar(
            @PathVariable String folio,
            @RequestPart("datos") QuejaEditarDTO editarDTO,
            @RequestPart(value = "nuevosArchivos", required = false) List<MultipartFile> archivos) {

        try {
            // Spring busca en la "bitácora" del guardia quién es el dueño del token actual
            Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long usuarioId = usuarioAutenticado.getId();            quejaService.editarQueja(folio, usuarioId, editarDTO, archivos);
            return ResponseEntity.ok("Queja actualizada correctamente");
        } catch (RuntimeException e) {
            // Esto atrapará el error de "No se puede editar una queja cancelada"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/borrar/{folio}")
    public ResponseEntity<?> eliminar(@PathVariable String folio) {
        try {
            Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long usuarioId = usuarioAutenticado.getId();

            quejaService.eliminarQueja(folio, usuarioId);
            return ResponseEntity.ok("La queja ha sido cancelada exitosamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


    /**
     * Descarga autenticada del acuse de recibo de una queja.
     * Valida que el JWT corresponda al dueño de la queja.
     *
     * Pendiente: cuando se integre generación dinámica de PDF (iText/OpenPDF),
     * solo se reemplaza el cuerpo de {@link #cargarPdfAcuse()} — el endpoint
     * y la URL no cambian.
     */
    @GetMapping("/{folio}/acuse")
    public ResponseEntity<byte[]> descargarAcuse(@PathVariable String folio) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getQuejoso() == null
                || !queja.getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para descargar este acuse");
        }

        return construirRespuestaAcuse(folio);
    }

    /**
     * Descarga pública del acuse de recibo. Valida que el folio y correo
     * coincidan con los registrados en la queja, sin requerir JWT.
     * Usado desde la pantalla de éxito y el modal de seguimiento del home.
     */
    @GetMapping("/{folio}/acuse/publico")
    public ResponseEntity<byte[]> descargarAcusePublico(
            @PathVariable String folio,
            @RequestParam String correo) {

        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        boolean correoCoincide = queja.getCorreoQuejoso() != null
                && queja.getCorreoQuejoso().equalsIgnoreCase(correo);

        if (!correoCoincide) {
            throw new RuntimeException("No tienes permiso para descargar este acuse");
        }

        return construirRespuestaAcuse(folio);
    }

    /**
     * Construye la respuesta HTTP con el PDF del acuse.
     * El nombre del archivo incluye el folio para identificarlo fácilmente
     * en la carpeta de descargas del usuario.
     */
    private ResponseEntity<byte[]> construirRespuestaAcuse(String folio) {
        byte[] pdf = cargarPdfAcuse();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"acuse-" + folio + ".pdf\"")
                .body(pdf);
    }

    /**
     * Lee el PDF placeholder desde el classpath (resources/static/acuse-placeholder.pdf).
     *
     * MIGRACIÓN FUTURA: reemplazar este método con generación dinámica usando
     * iText o OpenPDF, poblando el PDF con los datos de la queja (folio, fecha,
     * asunto, nombre del quejoso, estatus actual). El resto del controlador no cambia.
     */
    private byte[] cargarPdfAcuse() {
        try {
            ClassPathResource resource = new ClassPathResource("static/acuse-placeholder.pdf");
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("El acuse no está disponible temporalmente. Intenta más tarde.");
        }
    }

    private Queja mappearDtoAEntidad(QuejaRegistroDTO dto) {
        Queja q = new Queja();
        q.setAsunto(dto.getAsunto());
        q.setDescripcion(dto.getDescripcionHechos());
        q.setUnidaddondeOcurrio(dto.getUnidadAcademica());
        q.setFechaHechos(dto.getFechaHechos());
        q.setNombreDenunciado(dto.getNombreDenunciado() + " " + dto.getPrimerApellidoDenunciado());

        // --- DATOS DEL QUEJOSO (Persistencia sin cuenta) ---
        q.setCorreoQuejoso(dto.getCorreo());
        q.setNombreQuejoso(dto.getNombreQuejoso() + " " + dto.getPrimerApellido()
                + (dto.getSegundoApellido() != null && !dto.getSegundoApellido().isBlank()
                   ? " " + dto.getSegundoApellido() : ""));
        q.setIdentificacionInstitucional(dto.getIdentificacionInstitucional());
        q.setTipoIdentificacion(dto.getTipoIdentificacion());
        q.setFechaNacimiento(dto.getFechaNacimiento());

        // --- LÓGICA DEL TUTOR (MQ-03) ---
        if (dto.getTutor() != null) {
            Tutor t = new Tutor();
            t.setNombre(dto.getTutor().getNombre());
            // Usamos los campos corregidos en la Entidad que coinciden con el DTO
            t.setPrimerApellido(dto.getTutor().getPrimerApellido());
            t.setSegundoApellido(dto.getTutor().getSegundoApellido());
            t.setParentesco(dto.getTutor().getParentesco());
            t.setCorreo(dto.getTutor().getCorreo());
            t.setTelefono(dto.getTutor().getTelefono());

            // Establecemos la relación bidireccional
            t.setQueja(q);
            q.setTutor(t);
        }

        return q;
    }
}