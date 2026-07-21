package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.BandejaNuevaDTO;
import ipn.escom.defensoria.subdefensoria.service.BandejaInvestigacionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/bandeja-nuevas")
public class BandejaInvestigacionController {

    private final BandejaInvestigacionService bandejaInvestigacionService;

    public BandejaInvestigacionController(BandejaInvestigacionService bandejaInvestigacionService) {
        this.bandejaInvestigacionService = bandejaInvestigacionService;
    }

    @GetMapping
    public List<BandejaNuevaDTO> obtenerQuejasNuevas() {
        return bandejaInvestigacionService.obtenerQuejasNuevas();
    }
}
