package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejaEntranteDTO;
import ipn.escom.defensoria.primercontacto.store.QuejaEnMemoriaStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Puerta de entrada para el área de Subdefensoría.
 * Subdefensoría (u otro servicio, corriendo en otro puerto) hace un
 * POST aquí cada vez que exista una queja nueva o actualizada que
 * deba pasar por Primer Contacto. Este servicio la guarda en memoria
 * y a partir de ahí la Bandeja de Análisis y el Expediente la
 * exponen normalmente.
 * Esta llamada es servidor-a-servidor (backend de Subdefensoría ->
 * backend de Primer Contacto), por lo que CORS no aplica aquí; CORS
 * solo es relevante para llamadas hechas desde un navegador.
 */
@RestController
@RequestMapping("/api/primer-contacto/subdefensoria")
public class IngestaSubdefensoriaController {

    private final QuejaEnMemoriaStore store;

    public IngestaSubdefensoriaController(QuejaEnMemoriaStore store) {
        this.store = store;
    }

    @PostMapping("/quejas")
    public ResponseEntity<ExpedienteAnalisisDTO> recibirQueja(
            @Valid @RequestBody QuejaEntranteDTO queja
    ) {
        ExpedienteAnalisisDTO guardada = store.registrar(queja);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    /**
     * Endpoint de diagnóstico: te permite ver, sin pasar por el
     * front, qué es lo que este servicio tiene almacenado en un
     * momento dado.
     */
    @GetMapping("/quejas")
    public Collection<ExpedienteAnalisisDTO> listarRecibidas() {
        return store.listarTodas();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "quejasEnMemoria", store.total()
        );
    }
}
