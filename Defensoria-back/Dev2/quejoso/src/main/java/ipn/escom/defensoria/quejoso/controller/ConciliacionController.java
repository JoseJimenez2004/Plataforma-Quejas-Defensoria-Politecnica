package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.AcuerdoPendienteDTO;
import ipn.escom.defensoria.quejoso.dto.ConciliacionDTO;
import ipn.escom.defensoria.quejoso.dto.RespuestaConciliacionDTO;
import ipn.escom.defensoria.quejoso.entity.EstatusQuejaEntity;
import ipn.escom.defensoria.quejoso.entity.Queja;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.repository.QuejaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quejoso/conciliacion")
public class ConciliacionController {

    @Autowired
    private QuejaRepository quejaRepository;

    /**
     * Lista las quejas del quejoso autenticado que tienen un acuerdo
     * de conciliación pendiente de respuesta (estatus PROCESO_CONCLUSION).
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<AcuerdoPendienteDTO>> listarPendientes() {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<AcuerdoPendienteDTO> resultado = quejaRepository
                .findAcuerdosPendientes(usuarioAutenticado.getId())
                .stream()
                .map(q -> AcuerdoPendienteDTO.builder()
                        .folio(q.getFolio())
                        .fechaRegistro(q.getFechaRegistro())
                        .asunto(q.getAsunto())
                        .unidadAcademica(q.getUnidaddondeOcurrio())
                        .build())
                .toList();

        return ResponseEntity.ok(resultado);
    }

    /**
     * Muestra la propuesta de acuerdo vinculada a una queja específica.
     * Por ahora retorna datos mock — cuando exista el microservicio de
     * defensores, este endpoint lo consultará para obtener el PDF real.
     */
    @GetMapping("/{folio}")
    public ResponseEntity<ConciliacionDTO> verPropuesta(@PathVariable String folio) {
        // Validar que la queja pertenezca al usuario autenticado
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getQuejoso() == null || !queja.getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para ver este acuerdo");
        }

        ConciliacionDTO mockData = ConciliacionDTO.builder()
                .folioQueja(folio)
                .tituloAcuerdo("ACUERDO DE CONCILIACIÓN")
                .contenidoHtml("<p>La Defensoría propone los siguientes puntos para resolver el conflicto, "
                        + "buscando una solución justa y equitativa para todas las partes involucradas. "
                        + "Este acuerdo busca fomentar un ambiente de respeto y sana convivencia dentro de la institución.</p>")
                .fechaPropuesta(queja.getFechaRegistro().toLocalDate().toString())
                .urlPdf("http://localhost:4200/assets/prueba_conciliacion.pdf") //Aqui se cambiara el link cuando se tenga el pdf ya que este el otro micro de defensoria
                .build();

        return ResponseEntity.ok(mockData);
    }

    /**
     * El quejoso acepta el acuerdo de conciliación.
     * (Pendiente: notificar al microservicio de defensores para que
     * actualice el estatus de la queja a FINALIZADA cuando exista).
     */
    @PostMapping("/{folio}/aceptar")
    public ResponseEntity<String> aceptar(@PathVariable String folio) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getQuejoso() == null || !queja.getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para responder este acuerdo");
        }

        queja.setEstatus(EstatusQuejaEntity.FINALIZADA);
        quejaRepository.save(queja);

        return ResponseEntity.ok("Acuerdo aceptado correctamente. El defensor será notificado.");
    }

    /**
     * El quejoso rechaza el acuerdo de conciliación con una justificación obligatoria.
     * De momento NO ESTA GUARDANDO EN LA BASE DE DATOS LA JUSTIFICACION
     */
    @PostMapping("/{folio}/rechazar")
    public ResponseEntity<String> rechazar(@PathVariable String folio,
                                           @RequestBody RespuestaConciliacionDTO dto) {
        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getQuejoso() == null || !queja.getQuejoso().getId().equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("No tienes permiso para responder este acuerdo");
        }

        if (dto.getJustificacion() == null || dto.getJustificacion().trim().isEmpty()) {
            throw new RuntimeException("La justificación del rechazo es obligatoria");
        }

        queja.setEstatus(EstatusQuejaEntity.EN_ANALISIS);
        quejaRepository.save(queja);

        return ResponseEntity.ok("Acuerdo rechazado. El defensor analizará tu justificación.");
    }
}
