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

    public DictamenPrimerContactoController(
            DictamenPrimerContactoService dictamenPrimerContactoService
    ) {
        this.dictamenPrimerContactoService =
                dictamenPrimerContactoService;
    }

    @PostMapping("/competente")
    public DictamenDTO registrarCompetencia(
            @Valid @RequestBody CompetenciaDTO dto
    ) {
        return dictamenPrimerContactoService
                .registrarCompetencia(dto);
    }

    @PostMapping("/improcedente")
    public DictamenDTO registrarImprocedencia(
            @Valid @RequestBody ImprocedenciaDTO dto
    ) {
        return dictamenPrimerContactoService
                .registrarImprocedencia(dto);
    }

    @GetMapping("/expediente/{expedienteId}")
    public DictamenDTO obtenerPorExpediente(
            @PathVariable Long expedienteId
    ) {
        return dictamenPrimerContactoService
                .obtenerPorExpediente(expedienteId);
    }

    @GetMapping("/folio/{folio}")
    public DictamenDTO obtenerPorFolio(
            @PathVariable String folio
    ) {
        return dictamenPrimerContactoService
                .obtenerPorFolio(folio);
    }
}