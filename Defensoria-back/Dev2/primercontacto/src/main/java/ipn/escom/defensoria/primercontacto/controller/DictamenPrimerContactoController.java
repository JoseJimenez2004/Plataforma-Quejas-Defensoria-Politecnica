package ipn.escom.defensoria.primercontacto.controller;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.service.DictamenPrimerContactoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/primer-contacto/dictamenes")
public class DictamenPrimerContactoController {

    private final DictamenPrimerContactoService dictamenPrimerContactoService;

    public DictamenPrimerContactoController(DictamenPrimerContactoService dictamenPrimerContactoService) {
        this.dictamenPrimerContactoService = dictamenPrimerContactoService;
    }

    @PostMapping("/competente")
    public DictamenDTO registrarCompetencia(
            @Valid @RequestBody CompetenciaDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return dictamenPrimerContactoService.registrarCompetencia(dto, token);
    }

    @PostMapping("/improcedente")
    public DictamenDTO registrarImprocedencia(
            @Valid @RequestBody ImprocedenciaDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        return dictamenPrimerContactoService.registrarImprocedencia(dto, token);
    }

    @GetMapping("/queja/{quejaId}")
    public DictamenDTO obtenerPorQueja(
            @PathVariable Long quejaId
    ) {
        return dictamenPrimerContactoService.obtenerPorQueja(quejaId);
    }

    @GetMapping("/folio/{folio}")
    public DictamenDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return dictamenPrimerContactoService.obtenerPorFolio(folio);
    }
}