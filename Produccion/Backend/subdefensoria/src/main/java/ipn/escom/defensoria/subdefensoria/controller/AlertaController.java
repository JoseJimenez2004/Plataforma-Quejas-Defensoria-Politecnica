package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.dto.AlertaVencimientoDTO;
import ipn.escom.defensoria.subdefensoria.service.AlertaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/alertas")
public class AlertaController {

    private final AlertaService alertaService;
    private final AbogadoSesionService abogadoSesion;

    public AlertaController(AlertaService alertaService, AbogadoSesionService abogadoSesion) {
        this.alertaService = alertaService;
        this.abogadoSesion = abogadoSesion;
    }

    @GetMapping("/vencidos")
    public List<AlertaVencimientoDTO> obtenerVencidos(Authentication authentication) {
        return alertaService.obtenerVencidos(abogadoSesion.visibles(authentication));
    }
}
