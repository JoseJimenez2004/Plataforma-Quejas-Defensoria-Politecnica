package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.ExpedienteEntranteDTO;
import ipn.escom.defensoria.subdefensoria.dto.ExpedienteInvestigacionDTO;
import ipn.escom.defensoria.subdefensoria.service.IngestaExpedienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Puerta de entrada para el area de Primer Contacto: hace un POST
 * aqui cada vez que emite un acuerdo de admision y turna el
 * expediente a la Subdefensoria (Abogado Asesor) que corresponda.
 * Llamada servidor-a-servidor (Primer Contacto -> Subdefensoria), no
 * pasa por CORS.
 */
@RestController
@RequestMapping("/api/subdefensoria/ingesta")
public class IngestaExpedienteController {

    private final IngestaExpedienteService ingestaExpedienteService;

    public IngestaExpedienteController(IngestaExpedienteService ingestaExpedienteService) {
        this.ingestaExpedienteService = ingestaExpedienteService;
    }

    @PostMapping("/expedientes")
    public ResponseEntity<ExpedienteInvestigacionDTO> recibirExpediente(
            @Valid @RequestBody ExpedienteEntranteDTO expediente
    ) {
        ExpedienteInvestigacionDTO guardado = ingestaExpedienteService.recibirExpediente(expediente);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}
