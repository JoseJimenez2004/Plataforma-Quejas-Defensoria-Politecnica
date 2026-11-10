package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.dto.ControlPlazoDTO;
import ipn.escom.defensoria.subdefensoria.service.ControlPlazoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/control-plazos")
public class ControlPlazoController {

    private final ControlPlazoService controlPlazoService;
    private final AbogadoSesionService abogadoSesion;

    public ControlPlazoController(ControlPlazoService controlPlazoService, AbogadoSesionService abogadoSesion) {
        this.controlPlazoService = controlPlazoService;
        this.abogadoSesion = abogadoSesion;
    }

    @GetMapping
    public List<ControlPlazoDTO> obtenerSemaforo(Authentication authentication) {
        return controlPlazoService.obtenerSemaforo(abogadoSesion.visibles(authentication));
    }
}
