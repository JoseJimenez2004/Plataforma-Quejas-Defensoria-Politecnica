package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.AnalistaSesionDTO;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.service.AnalistaAutenticadoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/primer-contacto/analistas")
public class AnalistaSesionController {

    private final AnalistaAutenticadoService analistaAutenticadoService;

    public AnalistaSesionController(
            AnalistaAutenticadoService analistaAutenticadoService
    ) {
        this.analistaAutenticadoService = analistaAutenticadoService;
    }

    /*
     * Analista del JWT actual.
     */
    @GetMapping("/yo")
    public AnalistaSesionDTO yo(Authentication authentication) {

        PersonalAdministrativo analista =
                analistaAutenticadoService.obtenerAnalista(authentication);

        return AnalistaSesionDTO.builder()
                .id(analista.getId())
                .nombreCompleto(analista.getNombreCompleto())
                .correo(analista.getCorreoInstitucional())
                .build();
    }
}
