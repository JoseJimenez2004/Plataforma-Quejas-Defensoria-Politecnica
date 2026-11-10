package ipn.escom.defensoria.subdefensoria.controller;

import org.springframework.security.core.Authentication;
import ipn.escom.defensoria.subdefensoria.service.AbogadoSesionService;
import ipn.escom.defensoria.subdefensoria.dto.BandejaNuevaDTO;
import ipn.escom.defensoria.subdefensoria.service.BandejaInvestigacionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subdefensoria/bandeja-nuevas")
public class BandejaInvestigacionController {

    private final BandejaInvestigacionService bandejaInvestigacionService;
    private final AbogadoSesionService abogadoSesion;

    public BandejaInvestigacionController(BandejaInvestigacionService bandejaInvestigacionService, AbogadoSesionService abogadoSesion) {
        this.bandejaInvestigacionService = bandejaInvestigacionService;
        this.abogadoSesion = abogadoSesion;
    }

    @GetMapping
    public List<BandejaNuevaDTO> obtenerQuejasNuevas(Authentication authentication) {
        return bandejaInvestigacionService.obtenerQuejasNuevas(abogadoSesion.visibles(authentication));
    }
}
