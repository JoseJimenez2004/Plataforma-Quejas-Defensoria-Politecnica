package ipn.escom.defensoria.subdefensoria.controller;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.service.AcuerdoConclusionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdefensoria/acuerdos-conclusion")
public class AcuerdoConclusionController {

    private final AcuerdoConclusionService acuerdoConclusionService;

    public AcuerdoConclusionController(AcuerdoConclusionService acuerdoConclusionService) {
        this.acuerdoConclusionService = acuerdoConclusionService;
    }

    /** TS-07/08: redactar y, si concluir=true, cerrar el expediente (sin escalar a Defensoria). */
    @PostMapping
    public AcuerdoConclusionDTO guardarOConcluir(@Valid @RequestBody CrearAcuerdoConclusionDTO dto) {
        return acuerdoConclusionService.guardarOConcluir(dto);
    }

    @GetMapping("/expediente/{expedienteId}")
    public AcuerdoConclusionDTO obtenerPorExpediente(@PathVariable Long expedienteId) {
        return acuerdoConclusionService.obtenerPorExpediente(expedienteId);
    }
}
