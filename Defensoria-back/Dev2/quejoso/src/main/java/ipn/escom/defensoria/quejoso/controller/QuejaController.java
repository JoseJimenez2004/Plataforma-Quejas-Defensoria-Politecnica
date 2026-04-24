package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.EvidenciaDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaRegistroDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaSeguimientoDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaEditarDTO;
import ipn.escom.defensoria.quejoso.entity.Queja;
import ipn.escom.defensoria.quejoso.service.QuejaService;
import ipn.escom.defensoria.quejoso.entity.Tutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/quejoso/quejas")
public class QuejaController {

    @Autowired
    private QuejaService quejaService;

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

        // 2. Validamos los 30MB y preparamos las evidencias (si existen)
        if (archivos != null && !archivos.isEmpty()) {
            quejaService.validarYGuardarEvidencias(nuevaQueja, archivos);
        }

        // 3. Guardamos todo en la BD (Genera folio y vincula cuenta automáticamente)
        Queja quejaGuardada = quejaService.registrarQueja(
                nuevaQueja,
                registroDTO.getIdentificacionInstitucional(),
                registroDTO.getCorreo()
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
                                .map(e -> new EvidenciaDTO(e.getNombreArchivo(), e.getUrlAlmacenamiento()))
                                .collect(java.util.stream.Collectors.toList())
                        : new java.util.ArrayList<>())
                .build();
    }


    // VÍA 1: CONSULTA RÁPIDA (Pública - MQ-06)
// Se permite en SecurityConfig con .permitAll()
    @GetMapping("/seguimiento/publico")
    public ResponseEntity<?> consultarSeguimientoPublico(
            @RequestParam String folio,
            @RequestParam String correo) {

        return quejaService.obtenerSeguimiento(folio, correo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // VÍA 2: SEGUIMIENTO DETALLADO (Privada - Con cuenta)
// Requiere Token porque NO está en la lista de permitidos
    @GetMapping("/seguimiento/privado/{folio}")
    public ResponseEntity<QuejaSeguimientoDTO> consultarSeguimientoPrivado(@PathVariable String folio) {
        // Aquí el servicio debería validar que el dueño del Token sea el dueño de la queja
        return quejaService.obtenerSeguimiento(folio)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @PutMapping(value = "/editar/{folio}", consumes = {"multipart/form-data"})
    public ResponseEntity<String> editar(
            @PathVariable String folio,
            @RequestPart("datos") QuejaEditarDTO editarDTO,
            @RequestPart(value = "nuevosArchivos", required = false) List<MultipartFile> archivos) {

        Long usuarioId = 1L; // Temporal hasta tener el SecurityContext
        quejaService.editarQueja(folio, usuarioId, editarDTO, archivos);

        return ResponseEntity.ok("Queja actualizada correctamente");
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