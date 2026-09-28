package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.BusquedaAntecedentesDTO;
import ipn.escom.defensoria.primercontacto.service.antecedentes.AntecedentesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/primer-contacto/antecedentes")
public class AntecedentesController {

    private final AntecedentesService antecedentesService;

    public AntecedentesController(AntecedentesService antecedentesService) {
        this.antecedentesService = antecedentesService;
    }

    /*
     * Antecedentes del expediente (folio PC-...).
     *
     * GET /api/primer-contacto/antecedentes/PC-A1B2C3D4
     */
    @GetMapping("/{folio}")
    public BusquedaAntecedentesDTO buscar(@PathVariable String folio) {
        return antecedentesService.buscar(folio);
    }
}
