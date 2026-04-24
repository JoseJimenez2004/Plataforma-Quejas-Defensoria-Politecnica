package ipn.escom.defensoria.quejoso.controller;

import ipn.escom.defensoria.quejoso.dto.ConciliacionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quejoso/conciliacion")
public class ConciliacionController {

    /**
     * MQ-19: Muestra la propuesta vinculada a una queja específica.
     * Al no tener botones de respuesta, es un GET de visualización.
     */
    @GetMapping("/{folio}")
    public ResponseEntity<ConciliacionDTO> verPropuesta(@PathVariable String folio) {
        // Aquí llamaríamos a un service que consulte al micro de defensores
        ConciliacionDTO mockupData = ConciliacionDTO.builder()
                .folioQueja(folio)
                .tituloAcuerdo("ACUERDO DE CONCILIACIÓN")
                .contenidoHtml("<p>La Defensoría propone los siguientes puntos...</p>")
                .fechaPropuesta("2026-04-23")
                .build();

        return ResponseEntity.ok(mockupData);
    }
}