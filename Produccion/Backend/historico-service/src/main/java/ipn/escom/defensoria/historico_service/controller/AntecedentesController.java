package ipn.escom.defensoria.historico_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ipn.escom.defensoria.historico_service.dto.QuejaHistoricaDTO;
import ipn.escom.defensoria.historico_service.service.QuejaHistoricaService;

/**
 * Endpoints servidor a servidor. NO llevan JWT: los consume revision-service (para unir
 * estos antecedentes con los de defensoria_db antes de mostrárselos al recepcionista) y el
 * proceso que alimenta el modelo de búsqueda de antecedentes.
 *
 * Mismo patrón que /api/primer-contacto/ingesta y /api/subdefensoria/ingesta: quedan fuera
 * del JWT pero los puertos 8082-8092 solo son alcanzables desde la VPS frontend y desde la
 * propia VPS de backend (restricción por IP de origen en el firewall de Hostinger).
 */
@RestController
@RequestMapping("/api/historico/interno")
public class AntecedentesController {

    private final QuejaHistoricaService service;

    public AntecedentesController(QuejaHistoricaService service) {
        this.service = service;
    }

    /**
     * Antecedentes de una persona por su identificación, aparezca como quejoso o como
     * denunciado. Devuelve la forma del contrato de antecedentes, la misma que entrega
     * queja-service para las quejas del sistema.
     */
    @GetMapping("/antecedentes")
    public ResponseEntity<List<QuejaHistoricaDTO>> antecedentes(
            @RequestParam(required = false) String tipoIdentificacion,
            @RequestParam String numeroIdentificacion) {

        return ResponseEntity.ok(
                service.antecedentesPorIdentificacion(tipoIdentificacion, numeroIdentificacion));
    }

    /** Respaldo cuando el registro no trae boleta ni número de empleado. */
    @GetMapping("/antecedentes/por-nombre")
    public ResponseEntity<List<QuejaHistoricaDTO>> antecedentesPorNombre(
            @RequestParam String nombre,
            @RequestParam String apellido1) {

        return ResponseEntity.ok(service.antecedentesPorNombre(nombre, apellido1));
    }

    /**
     * Dataset completo en el formato del contrato. Lo consume el modelo de búsqueda de
     * antecedentes. Devuelve todo de una vez: el histórico es un conjunto acotado que se
     * carga una sola vez y casi no crece.
     */
    @GetMapping("/exportar")
    public ResponseEntity<List<QuejaHistoricaDTO>> exportar() {
        return ResponseEntity.ok(service.exportarTodas());
    }
}
