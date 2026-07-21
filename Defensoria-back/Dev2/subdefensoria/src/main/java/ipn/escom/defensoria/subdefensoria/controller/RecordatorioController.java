package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.GenerarRecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.dto.RecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.service.RecordatorioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    public RecordatorioController(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    /** Boton "Enviar Recordatorio Firmado" en P15.D. */
    @PostMapping
    public RecordatorioDTO generarRecordatorio(@Valid @RequestBody GenerarRecordatorioDTO dto) {
        return recordatorioService.generarRecordatorio(dto);
    }
}
