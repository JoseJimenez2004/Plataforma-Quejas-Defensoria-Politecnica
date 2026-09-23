package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.AlertaVencimientoDTO;
import ipn.escom.defensoria.subdefensoria.service.AlertaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping("/vencidos")
    public List<AlertaVencimientoDTO> obtenerVencidos() {
        return alertaService.obtenerVencidos();
    }
}
