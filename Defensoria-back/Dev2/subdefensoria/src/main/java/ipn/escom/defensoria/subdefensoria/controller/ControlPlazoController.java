package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.ControlPlazoDTO;
import ipn.escom.defensoria.subdefensoria.service.ControlPlazoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/control-plazos")
public class ControlPlazoController {

    private final ControlPlazoService controlPlazoService;

    public ControlPlazoController(ControlPlazoService controlPlazoService) {
        this.controlPlazoService = controlPlazoService;
    }

    @GetMapping
    public List<ControlPlazoDTO> obtenerSemaforo() {
        return controlPlazoService.obtenerSemaforo();
    }
}
