package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.dto.GenerarRecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.dto.RecordatorioDTO;
import ipn.escom.defensoria.subdefensoria.service.RecordatorioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;
    private final AbogadoSesionService abogadoSesion;

    public RecordatorioController(RecordatorioService recordatorioService, AbogadoSesionService abogadoSesion) {
        this.recordatorioService = recordatorioService;
        this.abogadoSesion = abogadoSesion;
    }

    /** Boton "Enviar Recordatorio Firmado" en P15.D. */
    @PostMapping
    public RecordatorioDTO generarRecordatorio(@Valid @RequestBody GenerarRecordatorioDTO dto,
                                               Authentication authentication) {
        abogadoSesion.exigirOficio(dto.getOficioId(), authentication);
        return recordatorioService.generarRecordatorio(dto);
    }

    /* Recordatorios ya enviados de un oficio (para el historial y sus PDF). */
    @GetMapping("/oficio/{oficioId}")
    public java.util.List<RecordatorioDTO> porOficio(@PathVariable Long oficioId) {
        return recordatorioService.porOficio(oficioId);
    }
}
