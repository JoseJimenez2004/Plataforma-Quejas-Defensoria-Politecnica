package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.entity.Expediente;
import ipn.escom.defensoria.primercontacto.repository.ExpedienteRepository;
import ipn.escom.defensoria.primercontacto.service.PrimerContactoService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/primer-contacto")
@RequiredArgsConstructor
public class PrimerContactoController {

    private final PrimerContactoService service;
    private final ExpedienteRepository repository;

    // --- BANDEJA DE ANÁLISIS ---
    @GetMapping("/bandeja")
    public ResponseEntity<List<Expediente>> listarBandeja() {
        return ResponseEntity.ok(service.obtenerBandejaAnalisis());
    }

    // --- CREACIÓN DE DENUNCIA 
    @PostMapping("/expedientes")
    public ResponseEntity<Expediente> crearExpediente(@RequestBody Expediente expediente) {
        if (expediente.getFechaIngreso() == null) {
            expediente.setFechaIngreso(LocalDateTime.now());
        }
        if (expediente.getEstatus() == null) {
            expediente.setEstatus("PENDIENTE");
        }
        return ResponseEntity.ok(repository.save(expediente));
    }

    // --- DETALLE DEL EXPEDIENTE ---
    @GetMapping("/expedientes/{folio}")
    public ResponseEntity<Expediente> verDetalle(@PathVariable String folio) {
        return ResponseEntity.ok(service.obtenerDetalleExpediente(folio));
    }

    // --- DICTAMEN (
    @PatchMapping("/expedientes/{folio}/dictamen")
    public ResponseEntity<Expediente> procesarDictamen(
            @PathVariable String folio,
            @RequestParam boolean esCompetencia,
            @RequestHeader("X-User-Role") String cargo,
            @RequestBody(required = false) String motivo) {
        return ResponseEntity.ok(service.dictaminarCompetencia(folio, esCompetencia, motivo, cargo));
    }

    // --- FORMALIZACIÓN ---
    @PostMapping("/expedientes/{folio}/formalizar")
    public ResponseEntity<Void> formalizar(
            @PathVariable String folio,
            @RequestHeader("X-User-Role") String cargo,
            @RequestBody String contenido) {
        service.formalizarYNotificar(folio, contenido, cargo);
        return ResponseEntity.noContent().build();
    }
}